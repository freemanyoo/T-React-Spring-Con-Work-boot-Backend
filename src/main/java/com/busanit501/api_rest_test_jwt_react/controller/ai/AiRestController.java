package com.busanit501.api_rest_test_jwt_react.controller.ai;

import com.busanit501.api_rest_test_jwt_react.dto.ai.image.AiPredictionResponseDTO;
import com.busanit501.api_rest_test_jwt_react.service.ai.AiUploadService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/ai")
@Log4j2
public class AiRestController {

    private final AiUploadService aiUploadService;

    // 다른 서버에 HTTP 요청을 보내기 위한 도구
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public AiRestController(AiUploadService aiUploadService) {
        this.aiUploadService = aiUploadService;
    }

    @PostMapping("/predict/{teamNo}")
    public AiPredictionResponseDTO uploadImage(
            @PathVariable int teamNo,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        // Django 서버로 이미지 전송 및 응답 처리
        //
        log.info("image 확인 : " + image);
        AiPredictionResponseDTO responseDTO = aiUploadService.sendImageToDjangoServer(image.getBytes(), image.getOriginalFilename(), teamNo);

        // PredictionResponseDTO 객체를 JSON으로 반환
        return responseDTO;
//        return imageUploadService.sendImageToDjangoServer(image.getBytes(), image.getOriginalFilename());
    }

    // 프록시 기능을 하는 새로운 메서드 추가
    @GetMapping("/results/{filename:.+}") // 파일 이름에 점(.)이 포함될 수 있도록 정규식 추가
    public ResponseEntity<byte[]> proxyAiResults(@PathVariable String filename) {

        // 1. 요청을 전달할 플라스크 서버의 실제 주소를 만듭니다.
        String flaskUrl = "http://localhost:5000/results/" + filename;

        try {
            // 2. 플라스크 서버에 GET 요청을 보내고 응답(파일 데이터, 헤더 등)을 그대로 받습니다.
            ResponseEntity<byte[]> responseFromFlask = restTemplate.exchange(
                    flaskUrl,
                    HttpMethod.GET,
                    null,
                    byte[].class
            );

            // 3. 플라스크 서버의 응답을 클라이언트에게 그대로 전달합니다.
            return responseFromFlask;

        } catch (HttpStatusCodeException e) {
            // 4. 플라스크 서버가 404(파일 없음) 등 에러 코드를 반환한 경우,
            // 해당 에러 코드와 메시지를 클라이언트에게 그대로 전달합니다.
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsByteArray());
        } catch (Exception e) {
            // 5. 플라스크 서버가 꺼져있는 등 네트워크 오류가 발생한 경우
            log.error("Error proxying request: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE); // 503 에러 반환
        }
    }


}
