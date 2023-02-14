package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker idWorker;

    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        // 1.查询秒杀券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        // 2.判断是否在有效期内
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 2.1.尚未开始
            return Result.fail("活动尚未开始");
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 2.2.已经结束
            return Result.fail("活动已经结束");
        }

        // 3.查询是否还有库存
        if (seckillVoucher.getStock() < 1) {
            // 3.1.库存不足
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();

        // 加锁(根据userId加锁)，保证一人一单
        synchronized (userId.toString().intern()) {
            //获取代理对象，以免事务不生效
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId, userId);
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId, Long userId) {
        // 4.一人一单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("一个账户只能拥有一张");
        }

        // 4.减库存（乐观锁）
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock -1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0).update();
        if (!success) {
            return Result.fail("库存不足");
        }

        // 5.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = idWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        return Result.ok(orderId);

    }
}
