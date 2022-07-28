package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * xToOne(ManyToOne, OneToOne) 관계 최적화를 하려고 한다. -> xToOne인 관계에 대해서만 가져오려고 한다.
 * Order 를 조회하는데 Member와 Delivery에 대해서만 연관이 걸리도록 할거다.
 * Order -> Member
 * Order -> Delivery
 *
 * entity를 직접 노출했을때 , 필요없는 데이터가 노출된다.
 * 꼭 필요한 데이터만 노출시켜야 한다.
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {
    private final OrderRepository orderRepository;

    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * 주문 조회 api
     * V1. 엔티티 직접 노출 하면 많은 문제점 발생 (필요 없는 데이터가 노출)
     * - 양방향 관계 문제 발생 -> @JsonIgnore 처리 하면 프록시 객체가 응답 json에 들어가서 오류 발생
     * - Hibernate5Module 모듈 등록, LAZY=null 처리해서 조금 문제가 해소 되었지만 여전히 문제 발생
     *
     * 이러한 방법은 사용하면 안된다. entity를 직접 노출하지 말자
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch()); // 조건이 없어서 전체 조회
            for (Order order : all) {
                order.getMember().getName(); //Lazy 강제 초기화
                order.getDelivery().getAddress(); //Lazy 강제 초기환
            }
        return all;
    }


    /**
     * V2. 엔티티를 조회해서 DTO로 변환(fetch join 사용X)
     * - 원래는 추후 유지보수 성을 위해서 Result<T>로 감싸야 하지만 지금은 그렇게 X
     * - lazy 로딩으로 인한 데이터베이스 쿼리가 너무 많이 호출된다.
     * - 3개의 Entity를 건들게 된다. 결과적으로 3개의 데이블을 조회해야 하는 상황
     * - 처음에 Order에 대해서 Table 조회 -> 2개의 주문건수 확인
     *                                    -> 루프로 1개의 건수 확인 -> Member, Delivery 조회
     *                                    -> 루프로 1개의 건수 확인 -> Member, Delivery 조회
     *
     * - 총 5번의 쿼리가 나갔다.
     * - N + 1 문제 발생
     *
     * - 원래 우리 예상은 1번 Order 조회 2번 주문 건수 조회 -> 3개를 예상했지만
     *                 1번 Order 조회 -> 주문건수 마다 Member, Delivery 조회 -> 총 5개의 주문건수 확인
     *
     * - 단점: 지연로딩으로 쿼리 N번 호출 -> Fetch Join을 사용해야 한다.
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o)) // 객체를 생성자로 바로 넘긴다.
                .collect(toList());
        return result;
    }
    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate; //주문시간
        private OrderStatus orderStatus;
        private Address address;
        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // LAZY 초기화 , 프록시 객체가 아닌 실제 데이터를 담아서 넣는다. 원래는 Member가 lazy fetch이다.
            orderDate = order.getOrderDate(); // LAZY 초기화 , 프록시 객체만을 반환하면 Error가 발생한다.따라서 실제 데이터를 반환해야한다.
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }

    /**
     * V3. 엔티티를 조회해서 DTO로 변환(fetch join 사용 O )
     * - fetch join으로 쿼리 1번 호출 -> N + 1 문제점 해결
     *
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(toList());
        return result;
    }

    /**
     * V4. JPA에서 DTO로 바로 조회
     * - 쿼리 1번 호출
     * - select 절에서 원하는 데이터만 선택해서 조회
     *
     * - V3와 member , delivery에 대해서 join 하는건 같지만 , 유일하게 다른점은 select 절에서 dto에 맞게 원하는 데이터만 가져와서 반환한다.
     * - select절에서 원하는 데이터만 가져오기 때문에 V3보댜 성능 최적화를 할 수 있다.
     *
     *  - 몇가지의 단점이 존재 -> 재사용성이 떨어진다.
     *  - V3 같은 경우는 공용으로 많이 사용될 수 있다. -> 단지 Controller 단에서 원한는 데이터(DTO)로 바인딩을 해서 반환하면 된다.
     *  - V4 같은 경우는 딱 정해진 DTO로만 반환하기 때문에 범용성이 떨어진다. Repo에서 조회할때 쿼리가 지저분해진다.
     *
     *  - 추가적으로 순수Repo는 순수한 Entity를 저장하거나 찾을때 사용하는 목적을 가지고 있지 , api에 종속적인 서비스를 개발하기 위해서
     *  - 만든것이 아니다. 따라서 특정 Dto를 new operation을 이용해서 조회하는 경우는 dto 반환을 위한 새로운 Repo를 만들어서 개발해야 한다.
     *  - 리포지토리 재사용성이 떨어짐, API 스펙에 맞춘 코드가 리포지토리에 들어가는 단점
     *  - 새로운 Repo를 만들어서 해당 dto 조회용 메서드를 옮겼다.
     *
     *  - V3와 V4 같은 경우는, 사실 select에서 조회하는 필드에서 차이가 난다. 실무에서는 사실 select에 의해서 조회되는 필드의 개수보다
     *  - 몇개의 join table를 거쳤는지에 대해서 성능이 천지차이로 나타난다.
     *  - 물론 select절에서 20~30개의 필드가 나온다면 그땐 이야기가 또 달라진다.
     *
     *  쿼리 방식 선택 권장 순서
     * 1. 우선 엔티티를 DTO로 변환하는 방법을 선택한다. -> V2
     * 2. 필요하면 페치 조인으로 성능을 최적화 한다. 대부분의 성능 이슈가 해결된다. -> V3 -> 보통 여기서 거의 다 해결
     * 3. 그래도 안되면 DTO로 직접 조회하는 방법을 사용한다. -> V$
     * 4. 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template을 사용해서 SQL을 직접
     * 사용한다
     */
    @GetMapping("/api/v4/simple-orders")

    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }



}