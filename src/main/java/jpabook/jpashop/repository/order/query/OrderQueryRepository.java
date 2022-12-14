package jpabook.jpashop.repository.order.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    /**
     * OrderQueryRepository에서 OrderApiController에 있는 OrderDto를 가져다 사용하면, 참조를 하면
     * Repository에 Controller를 참조하게 되는 의존관계가 순환이 된다.
     *
     * Query: 루트 1번, 컬렉션 N 번 실행
     * ToOne(N:1, 1:1) 관계들을 먼저 조회하고, ToMany(1:N) 관계는 각각 별도로 처리한다.
     * 이런 방식을 선택한 이유는 다음과 같다.
     * ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다.
     * ToMany(1:N) 관계는 조인하면 row 수가 증가한다.
     * row 수가 증가하지 않는 ToOne 관계는 조인으로 최적화 하기 쉬우므로 한번에 조회하고, ToMany
     * 관계는 최적화 하기 어려우므로 findOrderItems() 같은 별도의 메서드로 조회한다
     */

    private final EntityManager em;
    /**
     * 컬렉션은 별도로 조회
     * Query: 루트 1번, 컬렉션 N 번
     * 단건 조회에서 많이 사용하는 방식
     */
    public List<OrderQueryDto> findOrderQueryDtos() {
        //루트 조회(toOne 코드를 모두 한번에 조회 , toMany에 대해서는 가져오지 않는다.)
        List<OrderQueryDto> result = findOrders();
        //루프를 돌면서 컬렉션 추가(추가 쿼리 실행 , 컬렉션에 대한 데이터를 추가적인 쿼리로 따로 가져오고, 앞서 가져왔던 OrderQueryDto에 setter로 추가한다.)
        result.forEach(o -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());
            o.setOrderItems(orderItems); // setter를 통해서 하나씩 넣어준다.
        });
        return result;
    }

    /**
     * 1:N 관계(컬렉션)를 제외한 나머지를 한번에 조회
     *
     * fetch join은 select 결과에서 항상 엔티티를 조회해야 합니다.
     * fetch join 기능은 엔티티 객체와 객체 그래프를 함께 조회하는 것이 목적입니다.
     * 반면에 일반 join은 데이터베이스가 제공하는 join과 동일한 기능입니다. 따라서 select에서 원하는 필드를 바로 조회할 수 있습니다.
     *
     * join이 되는 엔티티 필드가 select 프로젝션에 있으면 프록시 객체가 안 들어가고 헤당 값 DB 조회해서  영속성 컨텍스트에 값이 들어가는 건가요?
     * -> 영속성 컨텍스트에 보관되는 것은 엔티티만 보관됩니다. select 프로젝션에서 특정 필드를 찍으면 해당 필드들은 보관되지  않습니다
     */
    private List<OrderQueryDto> findOrders() {
        // jpql로 new operation을 사용하더라도 일반 SQL문과 같다. 리스트에 대해서 new operation을 사용할 순 없다. 데이터를 flat하게 한줄
        //로만 넣을 수 있다. orderItems 같은 경우는 일대다 이기 때문에 바로 flat하게 넣지 못한다. 컬렉션을 제외하고 쿼리를 날리고 끝을 낸다.
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.query.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                        " from Order o" +
                                " join o.member m" +
                                " join o.delivery d", OrderQueryDto.class)
                .getResultList();
    }

    /**
     * 1:N 관계인 orderItems 조회
     */
    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                        " from OrderItem oi" +
                                " join oi.item i" +
                                " where oi.order.id = : orderId", // 특정한 OrderItem의 orderId마다 데이터를 가져오기 위해서 사용한다.
                        OrderItemQueryDto.class)
                .setParameter("orderId", orderId)
                .getResultList(); // list로 반환한다.
    }





    /**
     * 최적화
     * Query: 루트 1번, 컬렉션 1번
     * 데이터를 한꺼번에 처리할 때 많이 사용하는 방식
     *
     */
    public List<OrderQueryDto> findAllByDto_optimization() {
        //루트 조회(toOne 코드를 모두 한번에 조회)
        List<OrderQueryDto> result = findOrders();

        //orderItem 컬렉션을 MAP 한방에 조회, 이전까지는 for문 순환을 돌면서 N +1 문제 발생
        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(result));

        //루프를 돌면서 컬렉션 추가(추가 쿼리 실행X) , key 값을 통해서 찾는다.
        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));
        return result;
    }

    private List<Long> toOrderIds(List<OrderQueryDto> result) {
        // 반환 값으로 Member가 주문한 Order의 고유 id 값이 들어간다.
        return result.stream()
                .map(o -> o.getOrderId())
                .collect(Collectors.toList());
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {

        List<OrderItemQueryDto> orderItems = em.createQuery(
                        "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                        " from OrderItem oi" +
                                " join oi.item i" +
                                " where oi.order.id in :orderIds", OrderItemQueryDto.class) // v4와 다른점은 jpql에서 orerItems가 가지고 있는 id를 in절로 한방 쿼리를 통해 가져오려고 한다. 마치 batch size를 우리가 custom 하는 느낌
                .setParameter("orderIds", orderIds)
                .getResultList();

        // 조금이라도 성능 최적화를 위해 list를 id로 묶어서 데이터를 모아준다.
        // groupby를 통해서 list를 map으로 바꿀 수 있다.
        return orderItems.stream().collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));
    }


    /**
     * OrderItems에 대해서 fetch join으로 한방 쿼리로 가져오려고 한다.
     * 하지만 이렇게 하면 페이징 처리는 못하게 된다는 단점이 있다 왜냐하면 Order에 대한 데이터가 OrerItems에 맞게끔
     * 데이터 뻥튀기 현상이 발생하기 때문이다.
     * @return
     */
    public List<OrderFlatDto> findAllByDto_flat() {
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.query.OrderFlatDto(o.id, m.name, o.orderDate, o.status, d.address, i.name, oi.orderPrice, oi.count)" +
                        " from Order o" +
                                " join o.member m" +
                                " join o.delivery d" +
                                " join o.orderItems oi" +
                                " join oi.item i", OrderFlatDto.class)
                .getResultList();
    }


}
