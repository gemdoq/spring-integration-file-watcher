package com.springboot.springintegrationfilewatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class SpringIntegrationFileWatcherApplication {

	private final String INPUT_DIR = "inputdir";
	private final String OUTPUT_DIR = "outputdir";
	private final String ANOTHER_OUTPUT_DIR = "anotheroutputdir";
	private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".png", ".gif");

	// 디렉터리에서 파일을 읽는 메시지 소스 정의
	@Bean
	public MessageSource<File> sourceDirectory() {
		FileReadingMessageSource messageSource = new FileReadingMessageSource();
		messageSource.setDirectory(new File(INPUT_DIR)); // INPUT_DIR을 감시
		return messageSource;
	}

	// 허용된 확장자목록에 속하는 파일만 필터링
	@Bean
	public GenericSelector<File> onlyAllowedExtensions() {
		return source -> {
			String fileName = source.getName().toLowerCase();
			// 파일 이름이 허용된 확장자 중 하나로 끝나는지 확인
			return ALLOWED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
		};
	}

	// 파일을 OUTPUT_DIR에 쓰는 핸들러 정의
	@Bean
	public MessageHandler targetDirectory() {
		FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(OUTPUT_DIR));
		handler.setFileExistsMode(FileExistsMode.REPLACE); // 파일이 존재하면 덮어씀
		handler.setExpectReply(false);
		return handler;
	}

	// 파일을 ANOTHER_OUTPUT_DIR에 쓰는 핸들러 정의
	@Bean
	public MessageHandler anotherTargetDirectory() {
		FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(ANOTHER_OUTPUT_DIR));
		handler.setFileExistsMode(FileExistsMode.REPLACE);
		handler.setExpectReply(false);
		return handler;
	}

	@Bean
	public MessageChannel holdingTank() {
		return new QueueChannel(10); // 메시지 버퍼 크기를 10으로 설정
	}

	// 파일을 읽어서 중간 채널("holdingTank")로 전달하는 플로우 정의
	@Bean
	public IntegrationFlow fileReader() {
		return IntegrationFlow.from(sourceDirectory(), configurer -> configurer.poller(Pollers.fixedDelay(1000))) // 1초마다 폴링
				.filter(onlyAllowedExtensions()) // 허용된 확장자목록에 속하는 파일만 통과
				.channel("holdingTank") // 중간 채널로 전달
				.get();
	}

	// 중간 채널("holdingTank")에서 파일을 읽어 OUTPUT_DIR로 쓰는 플로우 정의
	@Bean
	public IntegrationFlow fileWriter() {
		return IntegrationFlow.from("holdingTank") // holdingTank 채널에서 메시지를 수신
				.bridge(e -> e.poller(Pollers.fixedRate(Duration.ofSeconds(1), Duration.ofSeconds(20)))) // 1초 간격으로 파일 처리
				.handle(targetDirectory()) // OUTPUT_DIR에 파일 쓰기
				.get();
	}

	// 중간 채널("holdingTank")에서 파일을 읽어 ANOTHER_OUTPUT_DIR로 쓰는 또 다른 플로우 정의
	@Bean
	public IntegrationFlow anotherFileWriter() {
		return IntegrationFlow.from("holdingTank") // holdingTank 채널에서 메시지를 수신
				.bridge(e -> e.poller(Pollers.fixedRate(Duration.ofSeconds(2), Duration.ofSeconds(10)))) // 2초 간격으로 파일 처리
				.handle(anotherTargetDirectory()) // ANOTHER_OUTPUT_DIR에 파일 쓰기
				.get();
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringIntegrationFileWatcherApplication.class, args);
	}

}
