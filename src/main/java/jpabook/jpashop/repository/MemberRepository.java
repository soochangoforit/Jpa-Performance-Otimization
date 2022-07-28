package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository // component 대상이 된다.
public class MemberRepository {

    @PersistenceContext // 원래는 다 꺼내서 쓰고 해야하지만 , Spring이 다 알아서 처리를 해준다.
    // @Autowired도 Context 어노테이션 대신 사용할 수 있다. 그 이유는 spring data jpa가 지원을 해줘서 가능하다. , 그러면 MemberRepository위에 @AllargsConstructor도 적용 가능하다.
    private final EntityManager em; // 스프링이 EntityManager를 생성해서 Injection 해준다.

    public MemberRepository(EntityManager em){
        this.em = em;
    }

    public Long save(Member member){
        em.persist(member);
        return member.getId();
    }

    public Member findOne(Long id){ // 던건 조회
        return em.find(Member.class, id); // 첫번쨰 Type, 두번째 PK
    }

    public List<Member> findAll(){ // JPQL 은 Table이 아니라 Entity를 대상으로 한다.
        List<Member> result = em.createQuery("select m from Member m", Member.class).getResultList();// 첫번째가 JPQL , 두번째가 반환 Type
        return result;
    }

    public List<Member> findByName(String name){
        return em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
    }

}
