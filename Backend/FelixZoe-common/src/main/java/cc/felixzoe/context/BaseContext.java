package cc.felixzoe.context;

/**
 * 线程上下文 - 兼容虚拟线程
 * 注意: 虚拟线程不会复用 ThreadLocal，每次虚拟线程创建都有独立的 ThreadLocal 存储
 * 但仍然需要确保在拦截器 afterCompletion 中清理，防止内存泄漏
 */
public class BaseContext {

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Integer> roleThreadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

    public static void setCurrentRole(Integer role) {
        roleThreadLocal.set(role);
    }

    public static Integer getCurrentRole() {
        return roleThreadLocal.get();
    }

    public static void removeCurrentRole() {
        roleThreadLocal.remove();
    }

    /**
     * 清除所有上下文，在请求结束时调用
     */
    public static void clear() {
        threadLocal.remove();
        roleThreadLocal.remove();
    }
}
