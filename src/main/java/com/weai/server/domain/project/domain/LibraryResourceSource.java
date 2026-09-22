package com.weai.server.domain.project.domain;

/** 자료실 문서가 어디에서 왔는지 구분한다. */
public enum LibraryResourceSource {
	/** 자료실 화면에서 팀원이 직접 업로드. */
	MANUAL,
	/** 채팅/회의 문서 업로드에서 자동으로 동기화됨. */
	MEETING
}
