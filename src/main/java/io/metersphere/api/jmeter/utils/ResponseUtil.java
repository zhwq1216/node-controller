package io.metersphere.api.jmeter.utils;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.metersphere.api.jmeter.dto.RequestResultExpandDTO;
import io.metersphere.api.service.utils.ErrorReportLibraryUtil;
import io.metersphere.api.vo.ErrorReportLibraryParseVo;
import io.metersphere.dto.RequestResult;
import io.metersphere.dto.ResponseResult;
import io.metersphere.utils.JsonUtils;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 请求返回解析工具
 */
public class ResponseUtil {

    public static RequestResultExpandDTO parseByRequestResult(RequestResult baseResult) {
        baseResult = ResponseUtil.parseResponseBodyByHeader(baseResult);
        //解析是否含有误报库信息
        ErrorReportLibraryParseVo errorCodeDTO = ErrorReportLibraryUtil.parseAssertions(baseResult);
        RequestResult requestResult = errorCodeDTO.getResult();
        RequestResultExpandDTO expandDTO = new RequestResultExpandDTO();
        BeanUtils.copyProperties(requestResult, expandDTO);
        if (CollectionUtils.isNotEmpty(errorCodeDTO.getErrorCodeList())) {
            Map<String, String> expandMap = new HashMap<>();
            expandMap.put(ApiReportStatus.FAKE_ERROR.name(), errorCodeDTO.getErrorCodeStr());
            if (StringUtils.equalsIgnoreCase(errorCodeDTO.getRequestStatus(), ApiReportStatus.FAKE_ERROR.name())) {
                expandMap.put("status", ApiReportStatus.FAKE_ERROR.name());
            }
            expandDTO.setAttachInfoMap(expandMap);
        }
        if (StringUtils.equalsIgnoreCase(errorCodeDTO.getRequestStatus(), ApiReportStatus.FAKE_ERROR.name())) {
            expandDTO.setStatus(errorCodeDTO.getRequestStatus());
        }
        return expandDTO;
    }

    public static RequestResult parseResponseBodyByHeader(RequestResult requestResult) {
        if (requestResult != null && requestResult.getResponseResult() != null && StringUtils.isNoneBlank(requestResult.getResponseResult().getHeaders())) {
            String[] headerArr = StringUtils.split(requestResult.getResponseResult().getHeaders(), StringUtils.LF);
            String formatType = ResponseFormatType.RAW.toString();
            for (String header : headerArr) {
                String[] headerKeyValue = StringUtils.split(header, ":");
                if (headerKeyValue.length == 2 && StringUtils.equalsIgnoreCase(headerKeyValue[0], "Content-Type")) {
                    if (StringUtils.containsIgnoreCase(headerKeyValue[1], ResponseFormatType.XML.name())) {
                        formatType = ResponseFormatType.XML.name();
                    } else if (StringUtils.containsIgnoreCase(headerKeyValue[1], ResponseFormatType.JSON.name())) {
                        formatType = ResponseFormatType.JSON.name();
                    }
                    break;
                }
            }
            requestResult.setResponseResult(formatResponseBody(requestResult.getResponseResult(), formatType));
        }
        return requestResult;
    }

    private static ResponseResult formatResponseBody(ResponseResult responseResult, String formatType) {
        if (responseResult != null) {
            String rspBody = responseResult.getBody();
            if (StringUtils.equalsIgnoreCase(formatType, ResponseFormatType.XML.name())) {
                try {
                    rspBody = XMLUtil.formatXmlString(responseResult.getBody());
                } catch (Exception e) {
                    LoggerUtil.error("格式化xml返回值失败，请检查返回参数是否为xml格式!", e);
                }
            } else if (StringUtils.equalsIgnoreCase(formatType, ResponseFormatType.JSON.name())) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    // 配置四个空格的缩进
                    DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter(StringUtils.SPACE + StringUtils.SPACE, DefaultIndenter.SYS_LF);
                    DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
                    printer.indentObjectsWith(indenter); // Indent JSON objects
                    printer.indentArraysWith(indenter);  // Indent JSON arrays
                    rspBody = mapper.writer(printer).writeValueAsString(JsonUtils.parseObject(rspBody));
                } catch (Exception e) {
                    LoggerUtil.error("格式化json返回值失败，请检查返回参数是否为json格式!", e);
                }
            }
            responseResult.setBody(rspBody);
        }
        return responseResult;
    }
}
