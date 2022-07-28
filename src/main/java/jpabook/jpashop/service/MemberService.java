package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true) // JPA의 모든 데이터 변경이나 어떤 로직들은 가급적이면 Transactional 안에서 다 실행되어야 한다. LazyLoading이 가능하다.
//@RequiredArgsConstructor 는 final로 선언된 필드만 생성자의 파라미터로 넣어준다. 그리고 생성자가 1개이기 떄문에 알아서 Autowired가 붙는다.
public class MemberService {

    private final MemberRepository memberRepository; //변경될 일이 없기 때문에 final 넣는걸 추천한다.

    @Autowired // Spring이 떠서 Class 생성 시점에 injection해준다.
    public MemberService(MemberRepository memberRepository){
        this.memberRepository = memberRepository;
    }

    //회원 가입
    @Transactional // 데이터의 변경이 일어나는 경우는 Transactional안에서 동작, 그리고 최상위에 readOnly로 설정했기 때문에 나머지 조회 관련된 메소드는 readOnly가 적용됨.
    public Long join(Member member){

        validateDuplicateMember(member);//중복 회원 가입방지 메소드 , 현재 프로젝트에서는 단순히 중복된 이름으로 가입을 방지하려고 한다.
        memberRepository.save(member);
        return member.getId(); // 영속성 컨텍스트에 의해서 값이 들어가있다는것을 알 수 있다.
    }

    //중복 회원 가입방지 메소드
    public void validateDuplicateMember(Member member){
        List<Member> findMembers = memberRepository.findByName(member.getName());
        if(!findMembers.isEmpty()){
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    //회원 전체 조회
    public List<Member> findMembers(){
        return memberRepository.findAll();
    }

    //회원 단건 조회
    public Member findOne(Long memberId){
        return memberRepository.findOne(memberId);
    }


    /**
     * 회원 수정
     *
     * 변경 감지를 최대한 활용
     */
    @Transactional
    public void update(Long id, String name) {
        Member member = memberRepository.findOne(id);
        member.setName(name);
    }


}
