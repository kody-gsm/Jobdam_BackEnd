package com.example.kodyjobdam.form.event;

/**
 * 선생님이 폼을 공개했다.
 *
 * <p>폼은 자기가 어느 공고에 딸렸는지 모르고, 폼에서 공고를 직접 부르면 순환 참조가 된다.
 * 그래서 공고를 함께 공개하는 일은 이 이벤트를 받는 공고 쪽에서 한다.</p>
 */
public record FormPublishedEvent(Long formId) {
}
