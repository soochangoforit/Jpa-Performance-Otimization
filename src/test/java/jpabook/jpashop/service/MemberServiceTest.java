package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;


@RunWith(SpringRunner.class) // Spring을 띄워서 같이 실행하기 위해서
@SpringBootTest // SpringBoot을 띄운 상태에서 test 진행하기 위해서 이게 없으면 Autowired가 실패한다. 컨테이너 안에서 test를 돌린다.
@Transactional // @Transactional 이 Test Case에 있으면 기본적으로 새로운 Test를 할때 Rollback을 한다.
public class MemberServiceTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MemberService memberService;

    // 만약 Test Method에서 Rollback(false)를 넣는대신 이렇게도 사용할 수 있다.
    // @Autowired EntityManager em;
    // 이렇게 선언을 하고 @Test Method에서 then 부분에 해당하는 곳에 em.flush를 해주면 실제 insert문이 들어가는걸 확인할 수 있고, Test가 끝나면 다시 Rollback된다.

    @Test
    // @Rollback(false)로 하면은 Rollback안하고 그대로 commit 해버린다. 이렇게 하면 실제 insert문이 들어가는걸 확인할 수 있다. insert문으로 DB에 저장은 되었지만 다시 클래스 위에 있는 @Transactional에 의해서 Rollback이 된다. 기본적으로 Test에서는 하나의 객체?에 대해서 반복적인 작업이 필요하기 때문이다.
    // @Transactional를 통해서 Rollback을 하면 JPA 입장에서는 실제 DB로 넣을 이유가 없다. 어차피 DB 데이터를 삭제할것이기 때문에. 정확히 말하면 영속성 컨텍스트에서 flush가 일어나지 않는다.
    // 눈으로 직접 확인해보는것이 좋다. 따라서 @Rollback(false)를 해주고 DB에 확인을 해보자, 어차피 다음 Test가 진행되면 자연스레 transactional이 될것이다.
    public void 회원가입() throws Exception {
        //given
        Member member = new Member();
        member.setName("kim");

        //when
        Long savedId = memberService.join(member); // join -> save -> persist ( DB로 persist만 하고 실제 insert는 기본적으로 하지 않는다 ) commit이 되고 flush가 되면서 insert 된다.

        //then
        // em.flush(); @Autowired EntityManager em 와 함께 사용하여 insert문이 실제로 날라가는 것을 확인할 수 있다.
        assertEquals(member, memberRepository.findOne(savedId)); // 같은 Transaction안에서 같은 Entity(Pk , ID )가 똑같으면 같은 영속성 컨텍스트에서 똑같은 객체로 관리가 된다.
     }


    @Test
    public void 중복회원예외() throws Exception {
        //given
        Member member1 = new Member();
        member1.setName("kim");

        Member member2 = new Member();
        member2.setName("kim");

        //when
        memberService.join(member1);
        try{
            memberService.join(member2); // 에러가 발생해야 한다.
        }catch(IllegalStateException e){
            return;
        }

        //then
        Assert.fail("예외가 발생해야 한다."); // 이까지 온 다는 의미는 catch에서 잡아주지 못하고 fail를 내뱉었다는 의미이다.

     }

}