package edu.sandau.rest.resource;

import com.alibaba.fastjson.JSONObject;
import edu.sandau.entity.LoginUser;
import edu.sandau.rest.model.Page;
import edu.sandau.rest.model.User;
import edu.sandau.security.Auth;
import edu.sandau.security.SessionWrapper;
import edu.sandau.service.UserService;
import edu.sandau.utils.RedisConstants;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("user")
@Api(value = "用户接口")
@Auth
public class UserResource {
    @Autowired
    private UserService userService;
    @Autowired
    private SessionWrapper sessionWrapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @ApiOperation(value = "修改用户信息")
    @ApiResponses({
            @ApiResponse(code=400, message="修改失败"),
            @ApiResponse(code=200, message = "修改成功, 返回最新记录", response = User.class)
    })
    @POST
    @Path("update")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response changeUser(LoginUser loginUser) throws Exception {
        if (loginUser.getId() == null) {
            return Response.ok().status(Response.Status.BAD_REQUEST).build();
        }
        User user = userService.updateUser(loginUser);
        if (user == null) {
            return Response.ok().status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok(user).build();
    }

    @ApiOperation(value = "查询当前用户信息")
    @GET
    @Path("info")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getCurrentUser() throws Exception {
        User user = sessionWrapper.getCurrentUser();
        return Response.ok(user).build();
    }

    @ApiOperation(value = "分页查询用户", response = List.class)
    @GET
    @Path("list")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response list(Page page) throws Exception {
        if (page == null) {
            return Response.ok().status(Response.Status.BAD_REQUEST).build();
        }
        page = userService.getUsersByPage(page);
        return Response.ok(page).build();
    }

    @ApiOperation(value = "重置密码", response = Boolean.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "用户id", dataType = "Integer", required = true ),
            @ApiImplicitParam(name = "password", value = "新密码", dataType = "String", required = true )
    })
    @POST
    @Path("reset-password")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response resetPassword(User user) throws Exception {
        Integer id = user.getId();
        String password = user.getPassword();
        if (StringUtils.isEmpty(password) || id==null) {
            return Response.accepted(false).status(400).build();
        }
        boolean ok = userService.resetPassword(id, password);
        if ( ok ) {
            return Response.ok(true).build();
        }
        return Response.accepted(false).status(500).build();
    }

    @ApiOperation(value = "设置默认密码", response = List.class)
    @POST
    @Path("default-password")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response defaultPassword(User user) throws Exception {
        Integer id = user.getId();
        if (id==null) {
            return Response.accepted(false).status(400).build();
        }
        boolean ok = userService.resetPassword(id);
        if ( ok ) {
            return Response.ok(true).build();
        }
        return Response.accepted(false).status(500).build();
    }

    @ApiOperation(value = "获取所有在线用户", response = List.class)
    @GET
    @Path("online-user")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getAllOnlineUser() throws Exception {
        String prefix = RedisConstants.SESSION_ID + "*";
        // 获取所有的key
        Set<String> keys = redisTemplate.keys(prefix);
        List<User> users = new ArrayList<>();
        // 批量获取数据
        for (String key: keys) {
            String value = Objects.requireNonNull(redisTemplate.opsForHash().get(key, "user")).toString();
            User user = JSONObject.parseObject(value, User.class);
            users.add(user);
        }
        return Response.ok(users).build();
    }
}
