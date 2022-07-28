package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}