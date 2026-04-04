package com.weai.server.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "Paginated response payload")
public record PageResponse<T>(
	@Schema(description = "Current page content")
	List<T> content,

	@Schema(description = "Current page index", example = "0")
	int page,

	@Schema(description = "Requested page size", example = "20")
	int size,

	@Schema(description = "Total element count", example = "125")
	long totalElements,

	@Schema(description = "Total page count", example = "7")
	int totalPages,

	@Schema(description = "Whether this is the first page", example = "true")
	boolean first,

	@Schema(description = "Whether this is the last page", example = "false")
	boolean last
) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
			page.getContent(),
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.isFirst(),
			page.isLast()
		);
	}
}
