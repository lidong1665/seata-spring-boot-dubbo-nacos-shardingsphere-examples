package io.seata.samples.integration.account.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.samples.integration.account.entity.TAccount;
import io.seata.samples.integration.account.mapper.TAccountMapper;
import io.seata.samples.integration.common.dto.AccountDTO;
import io.seata.samples.integration.common.enums.RspStatusEnum;
import io.seata.samples.integration.common.response.ObjectResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * * @author lidong
 * @since 2019-09-04
 */
@Service
public class TAccountServiceImpl extends ServiceImpl<TAccountMapper, TAccount> implements ITAccountService {

    @Transactional
    @Override
    public ObjectResponse decreaseAccount(AccountDTO accountDTO) {
        Integer id = accountDTO.getUserId();
        double account = accountDTO.getAmount().doubleValue();
        int decreaseAccount = this.baseMapper.decreaseAccount(id,account);
        ObjectResponse<Object> response = new ObjectResponse<>();
        if (decreaseAccount > 0){
            response.setStatus(RspStatusEnum.SUCCESS.getCode());
            response.setMessage(RspStatusEnum.SUCCESS.getMessage());
            return response;
        }

        response.setStatus(RspStatusEnum.FAIL.getCode());
        response.setMessage(RspStatusEnum.FAIL.getMessage());
        return response;
    }
}
