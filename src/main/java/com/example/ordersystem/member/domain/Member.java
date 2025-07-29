package com.example.ordersystem.member.domain;

import com.example.ordersystem.member.dto.MemberResponse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
/// jpql을 제외하고 모든 조회쿼리에 where del_yn = "N"을 붙이는 효과
//@Where(clause = "del_yn = 'N'")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;
    @Builder.Default
    private String delYn = "N";
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    public MemberResponse fromEntity() {
        return MemberResponse.builder()
                .id(this.id)
                .name(this.name)
                .email(this.email)
                .build();
    }

    public void deleteMember() {
        this.delYn = "Y";
    }
}
