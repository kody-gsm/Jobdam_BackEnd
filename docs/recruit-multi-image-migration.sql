-- 채용 공고 이미지 여러 장 지원
-- 운영(prod)은 spring.jpa.hibernate.ddl-auto=validate 이므로 테이블이 자동 생성되지 않는다.
-- 배포 전에 이 스크립트를 먼저 실행해야 앱이 기동된다.
--
-- 한 장만 담던 recruit.image_url 대신 recruit_image 테이블에 올린 순서대로 담는다.

CREATE TABLE recruit_image (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    recruit_id  BIGINT       NULL,
    order_index INT          NOT NULL,
    image_url   VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recruit_image_recruit FOREIGN KEY (recruit_id) REFERENCES recruit (id)
) ENGINE = InnoDB;

-- 기존 공고의 이미지 한 장을 첫 번째 이미지로 옮긴다. 이미 옮긴 공고는 건너뛰므로 다시 실행해도 된다.
INSERT INTO recruit_image (recruit_id, order_index, image_url)
SELECT r.id, 1, r.image_url
FROM recruit r
WHERE r.image_url IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM recruit_image ri WHERE ri.recruit_id = r.id);

-- 확인: 두 값이 같아야 한다.
-- SELECT COUNT(*) FROM recruit WHERE image_url IS NOT NULL;
-- SELECT COUNT(*) FROM recruit_image;

-- 앱은 더 이상 recruit.image_url을 읽지 않는다. 이전 버전은 이 컬럼을 읽으므로
-- 이관을 확인하고 이전 버전으로 되돌릴 일이 없을 때 지운다.
-- ALTER TABLE recruit DROP COLUMN image_url;
