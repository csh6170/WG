package com.wegive.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice // 모든 컨트롤러에서 발생하는 에러를 여기서 잡습니다.
public class GlobalExceptionHandler {

    /**
     * 404 Not Found: 주소를 잘못 입력했을 때
     * (application.yml 설정이 추가로 필요할 수 있음)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handle404(NoHandlerFoundException e) {
        return "error/404"; // templates/error/404.html 로 이동
    }

    /**
     * 500 Internal Server Error: 그 외 모든 서버 오류 (NullPointer, DB 등)
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleAllExceptions(Exception e, Model model) {
        // 개발 단계에서는 에러 메시지를 보는 게 좋으므로 로그에 출력
        e.printStackTrace();

        // 화면에 에러 내용을 살짝 보여주고 싶다면 추가 (보안상 실제 배포 시엔 제거 추천)
        model.addAttribute("errorMessage", e.getMessage());

        return "error/500"; // templates/error/500.html 로 이동
    }

    /**
     * IllegalArgumentException: 잘못된 요청 (예: 존재하지 않는 상품 조회)
     * 400 Bad Request 로 처리하거나 500 페이지로 보내면서 메시지만 다르게 줄 수 있음
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, Model model) {
        model.addAttribute("errorMessage", e.getMessage());
        return "error/500";
    }
}