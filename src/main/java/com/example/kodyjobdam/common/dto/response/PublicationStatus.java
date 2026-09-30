package com.example.kodyjobdam.common.dto.response;

/**
 * 공고·폼을 학생 화면에 어떻게 보여줄지 나타내는 공개 상태. 공고와 폼이 함께 쓴다.
 *
 * <p>저장하지 않고 응답을 만들 때 계산한다. 저장된 상태(FormStatus·RecruitStatus)만으로는
 * 마감일이 지났는지 알 수 없어서, 저장된 상태와 마감일을 합쳐 이 값으로 내려준다.</p>
 *
 * <p>값 이름은 저장된 상태와 일부러 똑같이 맞췄다. 이름이 달라지면 이미 'PUBLISHED'를 보고 있는
 * 화면이 전부 깨지므로, 새로 늘어나는 것은 마감을 뜻하는 CLOSED뿐이다.</p>
 */
public enum PublicationStatus {
    DRAFT,      // 선생님이 작성·검토 중인 초안 (학생에게 보이지 않음)
    PUBLISHED,  // 공개되어 있고 마감 전 (학생이 열람·지원 가능)
    CLOSED      // 공개됐지만 마감됨 (마감일이 지났거나 선생님이 직접 마감)
}
