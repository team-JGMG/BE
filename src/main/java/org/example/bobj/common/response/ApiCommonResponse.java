package org.example.bobj.common.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.bobj.common.constants.ErrorCode;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiCommonResponse<T> {
    private String status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String code;

    public static <T> ApiCommonResponse<T> createSuccess(T data) {
        return ApiCommonResponse.<T>builder()
            .status(ApiStatus.SUCCESS.getStatus())
            .data(data == null? (T) new HashMap<>(): data)
            .build();
    }

    public static <T> ApiCommonResponse<T> createSuccessWithNoContent() {
        return ApiCommonResponse.<T>builder()
            .status(ApiStatus.SUCCESS.getStatus())
            .data((T) new HashMap<>())
            .build();
    }

    // Hibernate Validator에 의해 유효하지 않은 데이터로 인해 API 호출이 거부될때 반환
    public static ApiCommonResponse<?> createFail(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();

        List<ObjectError> allErrors = bindingResult.getAllErrors();
        for (ObjectError error : allErrors) {
            if (error instanceof FieldError) {
                errors.put(((FieldError) error).getField(), error.getDefaultMessage());
            } else {
                errors.put( error.getObjectName(), error.getDefaultMessage());
            }
        }

        return ApiCommonResponse.builder()
            .status(ApiStatus.FAIL.getStatus())
            .data(errors)
            .build();
    }

    // 예외 발생으로 API 호출 실패시 반환
    public static ApiCommonResponse<?> createError(String message) {
        return ApiCommonResponse.builder()
            .status(ApiStatus.ERROR.getStatus())
            .message(message)
            .build();
    }

    public static ApiCommonResponse<?> createErrorWithCode(String code, String message) {
        return ApiCommonResponse.builder()
            .status(ApiStatus.ERROR.getStatus())
            .message(message)
            .code(code)
            .build();
    }
    public static ApiCommonResponse<?> createErrorWithCode(ErrorCode code) {
        return ApiCommonResponse.builder()
            .status(ApiStatus.ERROR.getStatus())
            .message(code.getMsg())
            .code(code.getCode())
            .build();
    }
}