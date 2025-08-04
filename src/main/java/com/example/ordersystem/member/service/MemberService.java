package com.example.ordersystem.member.service;

import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.dto.MemberCreateRequest;
import com.example.ordersystem.member.dto.MemberLoginRequest;
import com.example.ordersystem.member.dto.MemberResponse;
import com.example.ordersystem.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long saveMember(final MemberCreateRequest memberCreateRequest) {
        if(memberRepository.findByEmail(memberCreateRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 회원 이메일 입니다.");
        }
        final Member member = memberCreateRequest.toEntity(passwordEncoder.encode(memberCreateRequest.getPassword()));
        return memberRepository.save(member).getId();
    }

    @Transactional
    public Member login(MemberLoginRequest memberLoginRequest) {
        Optional<Member> member = memberRepository.findByEmail(memberLoginRequest.getEmail());
        boolean check = true;
        if (!member.isPresent()) {
            check = false;
        } else {
            if (!passwordEncoder.matches(memberLoginRequest.getPassword(), member.get().getPassword())) {
                check = false;
            }
        }

        if (!check) {
            throw new IllegalArgumentException("email 또는 비밀번호가 일치하지 않습니다.");
        }

        return member.get();
    }

    @Transactional
    public List<MemberResponse> getMemberList() {
        return memberRepository.findAll().stream().map((Member::fromEntity)).toList();
    }

    @Transactional
    public MemberResponse getMemberDetail(String email) {
        return memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("이메일이 유효하지 않습니다.")).fromEntity();
    }

    @Transactional
    public void delete() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException(""));
        member.deleteMember();
    }
}
