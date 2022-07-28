package jpabook.jpashop.api;


import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MemberApiController {
    private final MemberService memberService;

    /**
     * 등록 V1: 요청 값으로 Member 엔티티를 직접 받는다.
     * 문제점
     * - 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
     * - 엔티티에 API 검증을 위한 로직이 들어간다. (@NotEmpty 등등)
     * - 실무에서는 회원 엔티티를 위한 API가 다양하게 만들어지는데, 한 엔티티에 각각의 API를
     * 위한 모든 요청 요구사항을 담기는 어렵다.
     * - 엔티티가 변경되면 API 스펙이 변한다.
     * 결론
     * - API 요청 스펙에 맞추어 별도의 DTO를 파라미터로 받는다.
     */
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * 등록 V2: 요청 값으로 Member 엔티티 대신에 별도의 DTO를 받는다.
     *  Entity의 필드명이 바뀌더라도 api 스펙이 바뀌지 않는다.
     *  api에 맞는 별도의 dto로 받아야 한다.
     */
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        // 클라이언트 요청을 dto로 받아서 처리하도록 한다.
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @Data
    static class CreateMemberRequest {
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }


    /**
     * 수정 API
     * 별도의 update를 위한 dto를 만들어준다.
     */
    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id,
                                               @RequestBody @Valid UpdateMemberRequest request) {

        // 커맨드랑 쿼리를 철저하게 분리한다. update는 update하는 쿼리만, return 값으로 수정된 member를 반환하지 않는다.
        // 커맨드는 update 쿼리처럼 변경성 메서드 update는 가급적이면 id만 반환하고 그렇지 않는 경우, void 반환
        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id); // 업데이트 된 Member를 조회해서 리턴한다.
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    /**
     * Member의 이름만 업데이트 하기 위한 RequestDto
     */
    @Getter // 필수 : Getter 필요
    static class UpdateMemberRequest {
        private String name;
    }
    @Getter // 필수 : Getter , AllArgs 필요
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }


    /**
     * 조회 V1: 응답 값으로 엔티티를 직접 외부에 노출한다.
     * 문제점
     * - 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
     * - 기본적으로 엔티티의 모든 값이 노출된다.
     * - 응답 스펙을 맞추기 위해 로직이 추가된다. 멤버를 조회하기 위한 목적인데 주문내역 order[]가 들어가버린다. (@JsonIgnore, 별도의 뷰 로직 등등)
     * - 엔티티에 JsonIgnore를 걸어서 제한 가능, 하지만 다른 api에 대해서는 문제점 발생
     * - 실무에서는 같은 엔티티에 대해 API가 용도에 따라 다양하게 만들어지는데, 한 엔티티에 각각의
     *   API를 위한 프레젠테이션 응답 로직을 담기는 어렵다.
     * - 엔티티가 변경되면 API 스펙이 변한다.
     * - 추가로 컬렉션을 직접 반환하면 항후 API 스펙을 변경하기 어렵다.(별도의 Result 클래스
     *   생성으로 해결)
     *
     * 결론
     * - API 응답 스펙에 맞추어 별도의 응답DTO를 반환한다.
     *
     * 조회 V1: 안 좋은 버전, 모든 엔티티가 노출, @JsonIgnore -> 이건 정말 최악, api가 이거하 나인가!화면에 종속적이지마라!
     * 화면에 종속적이게 entity를 변경하지 말자.
     *
     * 데이터를 반환할때 모든 데이터를 일반적인 배열에 담아서 반환하고자 했고 추후 특정한 필드를 추가적으로 넣어서 반환할때 json 스펙이 깨져버린다.
     * 따라서 반환시 특정한 응답 dto에 담아서 반환하도록 해야하자, 추가적인 응답 dto에 대하 요구사항이 있을때 json 스펙을 깨지않고
     * 반환할 수 있다.
     */
    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        return memberService.findMembers();
    }


    /**
     *  조회 V2: 응답 값으로 엔티티가 아닌 별도의 DTO를 반환한다.
     *
     */
    @GetMapping("/api/v2/members")
    public Result membersV2() {
        List<Member> findMembers = memberService.findMembers();
        //엔티티 -> DTO 변환
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());
        return new Result(collect);
    }

    /**
     * result라는 껍데기를 만들어줘서 반환한다.
     * json으로 "data" : [ ~ ] 형식으로 반환된다.
     * 한번 감싸줘야 한다. 바로 list 형식으로 반환을 해버리면, json 배열타입으로 나가기 때문에 유연성이 떨어진다.
     * @param <T>
     */
    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }
    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }


}