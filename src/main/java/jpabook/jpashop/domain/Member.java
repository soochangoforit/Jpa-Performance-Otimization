package jpabook.jpashop.domain;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    @NotEmpty(message = "회원 이름은 필수 입니다") // DB 제약 조건과는 다르게, controller단에서 해결하기 위해서 사용한다.
    private String name;

    @Embedded
    private Address address;

    // 여기서의 mappedBy의 member는 "Order 필드의 member"에 의해서 mapped 된다는 의미이다.
    @OneToMany(mappedBy = "member") // 하나의 회원이 여러 상품을 주문 , 연관관계의 주인이 order에 있다. order Table에 있는 member filed에 의해서 매핑이 된거야
    private List<Order> orders = new ArrayList<>(); // 읽기 전용이 된다.
}
