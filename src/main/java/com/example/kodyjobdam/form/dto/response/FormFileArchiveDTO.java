package com.example.kodyjobdam.form.dto.response;

import java.util.List;

/**
 * 폼 첨부 파일을 ZIP으로 묶을 목록.
 *
 * <p>DB에서 읽을 것은 미리 다 읽어 두고, 파일 내용은 트랜잭션 밖에서 저장소에서 읽어 흘려 보낸다.</p>
 */
public record FormFileArchiveDTO(String fileName, List<Entry> entries) {

    /**
     * @param name       ZIP 안의 파일 이름. 응답 엑셀의 파일 칸에 적힌 이름과 같다.
     * @param storedName 저장소 안의 상대 경로
     */
    public record Entry(String name, String storedName) {
    }
}
