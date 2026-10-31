package cn.learn.lfy.mtr.mapper;

import cn.learn.lfy.mtr.domain.model.AppUser;
import cn.learn.lfy.mtr.domain.model.UserCredential;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface AuthMapper {

    Optional<UserCredential> findCredentialByUsername(@Param("username") String username);

    Optional<AppUser> findUserById(@Param("userId") long userId);

    Optional<AppUser> findUserBySessionTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now
    );

    int insertSession(
            @Param("tokenHash") String tokenHash,
            @Param("userId") long userId,
            @Param("expiresAt") LocalDateTime expiresAt
    );

    int deleteSession(@Param("tokenHash") String tokenHash);

    int deleteExpiredSessions(
            @Param("userId") long userId,
            @Param("now") LocalDateTime now
    );
}
