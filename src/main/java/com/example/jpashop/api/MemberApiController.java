package com.example.jpashop.api;

import com.example.jpashop.modules.member.Member;
import com.example.jpashop.modules.member.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
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
     * 등록 v1
     * - 엔티티를 RequestBody에 직접 매핑
     * -> API 요청 스펙에 맞춰 별도의 DTO를 파라미터로 받는다.
     */
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * 등록 v2
     * - 요청 값으로 Member 엔티티 대신에 별도의 DTO를 받는다.
     */
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @Data
    class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

    @Data
    private class CreateMemberRequest {
        private String name;
    }

    /**
     * 수정
     */
    @PostMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id, @RequestBody @Valid UpdateMemberRequest request) {

        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    @Data
    private class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    private class UpdateMemberResponse {
        private Long id;
        private String name;
    }

    /**
     * 조회 v1
     * - 응답 값으로 엔티티를 직접 외부에 노출한다.
     * -> 엔티티 대신에 API 스펙에 맞는 별도의 DTO를 노출해야 한다.
     */
    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        // 컬렉션을 직접 반환하면 향후 API 스펙을 변경하기 어렵다.
        return memberService.findMembers();
    }

    /**
     * 조회 v2
     * - 응답 값으로 엔티티가 아닌 별도의 DTO를 반환한다.
     */
    public Result membersV2() {

        List<Member> findMembers = memberService.findMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(member -> new MemberDto(member.getName()))
                .collect(Collectors.toList());

        return new Result(collect);
    }

    // Result 클래스로 컬렉션을 감싸서 향후 필요한 필드를 추가할 수 있다.
    @Data
    @AllArgsConstructor
    class Result<T> {
        private T data;
    }

    @Data
    private class MemberDto {
        private String name;

        public MemberDto(String name) {
            this.name = name;
        }
    }
}
