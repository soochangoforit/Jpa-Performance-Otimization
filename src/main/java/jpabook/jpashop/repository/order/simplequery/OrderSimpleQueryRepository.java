package jpabook.jpashop.repository.order.simplequery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {
    private final EntityManager em;


    /**
     * - jpa는 기본적으로 entity 혹은 embeddable entity만을 반환한다.
     * - dto를 반환하기 위해서는 new operation을 활용해야 한다.
     * - OrderSimpleQueryDto의 생성자에 파라미터를 다 넘겨줘야 한다. -> 그냥 파라미터로 'o'를 넣어버리면 JPA에서는 이동할때 식별자만을 반환한다.
     *
     * - new 오퍼레이션을 사용할 때는 OrderSimpleQueryDto(o) 이렇게 넘기면 o의 식발자인 o.id만 넘어가게 됩니다.
     * - 따라서 OrderSimpleQueryDto(o.id)와 동일하다고 보시면 됩니다.
     *
     * - 그래서 생성자의 'o'에는 식별자만 들어갈것이다. -> 파라미터 하나 하나에 대해서 넣어주자.
     *
     * - v3는 엔티티를 조회하는 것이고,
     * - v4는 엔티티가 아닌 DTO로 바로 조회하는 방식입니다.
     * - fetch join은 JPA에서 지원하는 문법이고, 엔티티를 조회할 때만 사용할 수 있습니다. DTO를 조회할 때는 사용할 수 없습니다.
     * - fetch join을 사용하더라도 결국 관계형 데이터베이스에서 연관된 데이터를 조회할 때는 JOIN 구문을 사용하게 됩니다.
     *
     * @return Entity가 반환하는게 아니라, OrderSimpleQueryDto를 반환한다.
     */
    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                                " from Order o" +
                                " join o.member m" +
                                " join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }
}