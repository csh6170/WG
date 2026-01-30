package com.wegive.domain.home.controller;

import com.wegive.domain.item.entity.Item;
import com.wegive.domain.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ItemRepository itemRepository; // 서비스 거쳐도 되지만, 단순 조회라 바로 호출도 OK

    @GetMapping("/")
    public String home(Model model) {
        // 1. 상태가 'DELETE'나 'HIDDEN'이 아닌 정상 게시글만 가져오기
        // (Status 관리를 아직 안 했다면 findAllByOrderByCreatedAtDesc() 써도 됩니다)
        List<Item> items = itemRepository.findAllByStatusNotOrderByCreatedAtDesc("DELETE");

        // 2. 화면으로 데이터 보내기
        model.addAttribute("items", items);

        return "home"; // templates/home.html
    }
    @GetMapping("/test-error")
    public String throwError() {
        // 의도적으로 런타임 예외를 발생시킵니다.
        throw new RuntimeException("500 페이지 테스트를 위한 의도적인 에러입니다!");
    }
}