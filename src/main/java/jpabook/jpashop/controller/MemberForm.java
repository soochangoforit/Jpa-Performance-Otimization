package jpabook.jpashop.controller;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter @Setter
public class MemberForm {

    @NotEmpty(message = "회원 이름은 필수 입니다") //dependency가서 의존성 추가해줬음 validation
    private String name;

    private String city;
    private String street;
    private String zipcode;

}
