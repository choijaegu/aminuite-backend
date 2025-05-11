package com.example.political_chat_backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HomeController {

    private final TestMessageRepository testMessageRepository; // TestMessageRepository 의존성 주입을 위한 필드

    // 생성자를 통해 TestMessageRepository를 주입받습니다 (Constructor Injection)
    public HomeController(TestMessageRepository testMessageRepository) {
        this.testMessageRepository = testMessageRepository;
    }

    @GetMapping("/")
    public String home() {
        return "안녕하세요! 정치 게시판 백엔드 서버입니다. (Welcome to Political Chat Backend!)";
    }

    // 새로운 DB 테스트용 경로 추가
    @GetMapping("/test-db")
    public String testDb() {
        // 1. 새로운 TestMessage 객체를 만들고 내용을 설정합니다.
        TestMessage newMessage = new TestMessage("첫 번째 데이터베이스 테스트 메시지! 시간: " + System.currentTimeMillis());

        // 2. testMessageRepository를 사용하여 newMessage를 데이터베이스에 저장합니다.
        testMessageRepository.save(newMessage);

        // 3. 데이터베이스에 저장된 모든 TestMessage를 불러옵니다.
        List<TestMessage> allMessages = testMessageRepository.findAll();

        // 4. 결과를 문자열로 만들어서 반환합니다.
        StringBuilder response = new StringBuilder();
        response.append("<h1>DB 테스트 성공!</h1>");
        response.append("<p>총 저장된 메시지 수: ").append(allMessages.size()).append("</p>");
        response.append("<ul>");
        for (TestMessage msg : allMessages) {
            response.append("<li>").append(msg.toString()).append("</li>"); // TestMessage 클래스의 toString() 메소드가 호출됩니다.
        }
        response.append("</ul>");

        return response.toString();
    }
}
