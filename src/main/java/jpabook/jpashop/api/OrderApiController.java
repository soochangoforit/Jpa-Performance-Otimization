package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * V1. 엔티티 직접 노출
 * - 엔티티가 변하면 API 스펙이 변한다.
 * - 트랜잭션 안에서 지연 로딩 필요
 * - 양방향 연관관계 문제
 *
 * V2. 엔티티를 조회해서 DTO로 변환(fetch join 사용X)
 * - 트랜잭션 안에서 지연 로딩 필요
 * V3. 엔티티를 조회해서 DTO로 변환(fetch join 사용O)
 * - 페이징 시에는 N 부분을 포기해야함(대신에 batch fetch size? 옵션 주면 N -> 1 쿼리로 변경
 가능)
 *
 * V4. JPA에서 DTO로 바로 조회, 컬렉션 N 조회 (1 + N Query)
 * - 페이징 가능
 * V5. JPA에서 DTO로 바로 조회, 컬렉션 1 조회 최적화 버전 (1 + 1 Query)
 * - 페이징 가능
 * V6. JPA에서 DTO로 바로 조회, 플랫 데이터(1Query) (1 Query)
 * - 페이징 불가능...
 *
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    /**
     * V1. 엔티티 직접 노출
     * - Hibernate5Module 모듈 등록, LAZY=null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     *
     * 엔티티를 절대 반환하면 안된다. 올바르지 못한 컨트롤러이다.
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); //Lazy 강제 초기화 -> 초기화 한 이유는 프록시 객체가 들어갈 순 없기 때문에
            order.getDelivery().getAddress(); //Lazy 강제 초기화

            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName()); //Lazy 강제초기화 collect(Collectors.toList())하지 않아도
                                                                    // 해당 변경값이 orderItems에 알아서 담기게 된다.
        }
        return all;
    }


    /**
     * - 주문 조회 V2: 엔티티를 DTO로 변환
     * - 지연 로딩으로 너무 많은 SQL 실행
     * - 정팔 필요한 데이터만을 dto를 통해서 반환을 하고 있지만,
     * - 생성자 시점에서 dto 객체로 데이터 바인딩 하는 과정에서 프록시 객체를 사용해서 DB에 쿼리를 불러오기 때문에
     * - N + 1 문제 발생
     * - Entity 대신 Dto로 반환을 하더라도 , Entity를 Dto로 매핑하는것도 있으면 안된다. (지연로딩 사용)
     *
     * - SQL 실행 수
     * - order 1번
     * - member , address N번(order 조회 수 만큼)
     * - orderItem N번(order 조회 수 만큼)
     * - item N번(orderItem 조회 수 만큼)
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }


    @Data
    static class OrderDto {
        private Long orderId;
        private String name; // member의 이름을 프록시 객체로 가져오지 않고. 실제로 쿼리를 날려서 실제 데이터를 가져오려고 한다.
                             // 만약 Member 가져오려고 한다면 -> 순환 참조 에러가 발생한다.
        private LocalDateTime orderDate; //주문시간
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;
        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // 프록시 객체 초기화
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(toList());
        }
    }
    @Data
    static class OrderItemDto {
        private String itemName;//상품 명
        private int orderPrice; //주문 가격
        private int count; //주문 수량
        public OrderItemDto(OrderItem orderItem) { // orderItem이 가지고 있는 Order에 대해서는 다시 가져오지 않는다.
            itemName = orderItem.getItem().getName(); // 프록시 객체 초기화
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }


    /**
     * fetch join을 사용해서 주문 조회 V3
     * fetch join을 하더라도 OneToMany 관계에 의해서 데이터 뻥튀기 현상 발생
     * Order가 OrderItem의 개수에 맞게 뻥튀기 현상 발생 2개에서 4개로 증가
     * 실제 return 할때도 select가 Order table에 맞춰 있기 때문에,
     * 반환되는 데이터에는 Order에 대한 똑같은 값이 2번 나온다.
     * OrerItem의 크기에 맞게 inner join이 되기때문에 Order에 대해서는 2배의 뻥튀기 발생
     * DB 입장에서는 1 : '다' 가 있으면 '다'에 대해서 데이터가 증가하게 된다. -> distinct로 해결 가능
     *
     * - 결과적으로 1번의 쿼리가 나가게 된다.
     * - V2와 다른점은 객체 그래프를 가지고 fetch join을 했는지 안했는지에 대해서 차이가 난다.
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem(); // orders 에서도 2건이 나와야 하지만 4건이 나오고 있다.
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }


    /**
     * V3.1 엔티티를 조회해서 DTO로 변환 페이징 고려
     * - ToOne 관계만 우선 모두 페치 조인으로 최적화
     * - 컬렉션 관계는 hibernate.default_batch_fetch_size, @BatchSize로 최적화
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {

        // XToOne이 걸린 데이터들을 단순히 fetch join으로 필요한 데이터가 있는 테이블을 한방 쿼리로 조회한다.
        // 페이징에 영향을 주지 않기 때문에 -> 대신 orderItems에 대해서는 N +1 문제점이 발생한다.
        // List<Order> orders = orderRepository.findAllWithMemberDelivery();

        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }




}
