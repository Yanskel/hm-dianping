package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryAll() {
        // 1.从Redis获取店铺类型缓存(JSON)
        String listJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE);

        // 2.判断是否存在
        if (StrUtil.isNotBlank(listJson)) {
            // 3.存在，返回对应集合
            List<ShopType> shopTypeList = JSONUtil.toList(listJson, ShopType.class);
            return Result.ok(shopTypeList);
        }

        // 4.不存在，从数据库中获取
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();

        // 5.数据库中不存在，返回异常
        if (shopTypeList == null) {
            return Result.fail("商品类型显示异常");
        }

        // 6.将数据写入缓存
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE,JSONUtil.toJsonStr(shopTypeList));

        // 7.返回店铺类型集合
        return Result.ok(shopTypeList);
    }
}
