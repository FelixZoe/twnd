package cc.felixzoe.service;

import cc.felixzoe.dto.*;
import cc.felixzoe.vo.AdminLoginVO;
import cc.felixzoe.vo.AdminVO;

public interface AdminService {

    /**
     * 发送验证码（需验证用户名和密码）
     */
    void sendVerifyCode(String username, String password);

    /**
     * 管理员登录
     * @param adminLoginDTO
     * @return
     */
    AdminLoginVO login(AdminLoginDTO adminLoginDTO) throws Exception;

    /**
     * 获取管理员信息
     * @return
     */
    AdminVO getAdminById();

    /**
     * 管理员退出登录
     * @param adminLogoutDTO
     */
    void logout(AdminLogoutDTO adminLogoutDTO);

    /**
     * 管理员修改密码
     * @param adminChangePasswordDTO
     */
    void changePassword(AdminChangePasswordDTO adminChangePasswordDTO) throws Exception;

    /**
     * 管理员更改昵称
     * @param adminChangeNicknameDTO
     */
    void changeNickname(AdminChangeNicknameDTO adminChangeNicknameDTO);

    /**
     * 管理员换绑邮箱
     * @param adminChangeEmailDTO
     */
    void changeEmail(AdminChangeEmailDTO adminChangeEmailDTO);
}
