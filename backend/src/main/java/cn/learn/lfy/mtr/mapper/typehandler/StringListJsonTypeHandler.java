package cn.learn.lfy.mtr.mapper.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class StringListJsonTypeHandler extends BaseTypeHandler<List<String>> {
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<String> value, JdbcType jdbcType)
            throws SQLException {
        try {
            statement.setString(index, objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException exception) {
            throw new SQLException("无法序列化 MIME 类型列表", exception);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return parse(resultSet.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return parse(resultSet.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return parse(statement.getString(columnIndex));
    }

    private List<String> parse(String value) throws SQLException {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new SQLException("无法解析模板 MIME 类型列表", exception);
        }
    }
}
