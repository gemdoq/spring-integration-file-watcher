package com.springboot.springintegrationfilewatcher;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DirectoryInitializer {

	@PostConstruct
	public void createInputDirIfNotExists() {
		File inputDir = new File("inputdir");
		if (!inputDir.exists()) { // 디렉토리가 존재하지 않을 경우
			boolean created = inputDir.mkdir();
			if (created) {
				System.out.println("'inputdir' 생성 성공");
			} else {
				System.err.println("'inputdir' 생성 실패");
			}
		} else { // 디렉토리가 이미 존재하면
			System.out.println("이미 'inputdir' 존재");
		}
	}
}