package jpabook.jpashop.controller;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BookForm {

    private Long id; // 상품을 수정해야 하기 때문에 id값이 필요하다.

    //상품 공통 속성
    private String name;
    private int price;
    private int stockQuantity;

    private String author;
    private String isbn;

}
