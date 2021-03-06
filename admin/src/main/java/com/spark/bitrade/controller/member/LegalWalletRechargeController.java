package com.spark.bitrade.controller.member;

import com.mysema.commons.lang.Assert;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.spark.bitrade.constant.LegalWalletState;
import com.spark.bitrade.constant.PageModel;
import com.spark.bitrade.constant.TransactionType;
import com.spark.bitrade.controller.common.BaseAdminController;
import com.spark.bitrade.entity.MemberTransaction;
import com.spark.bitrade.model.screen.LegalWalletRechargeScreen;
import com.spark.bitrade.entity.LegalWalletRecharge;
import com.spark.bitrade.entity.MemberWallet;
import com.spark.bitrade.entity.QLegalWalletRecharge;
import com.spark.bitrade.service.LegalWalletRechargeService;
import com.spark.bitrade.service.MemberTransactionService;
import com.spark.bitrade.service.MemberWalletService;
import com.spark.bitrade.util.MessageResult;
import com.spark.bitrade.util.PredicateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

/**
 * rongyu
 */
@RestController
@RequestMapping("legal-wallet-recharge")
public class LegalWalletRechargeController extends BaseAdminController {
    @Autowired
    private LegalWalletRechargeService legalWalletRechargeService;
    @Autowired
    private MemberWalletService walletService;

    @Autowired
    private MemberTransactionService memberTransactionService ;

    @GetMapping("page")
    public MessageResult page(
            PageModel pageModel,
            LegalWalletRechargeScreen screen) {
        Predicate predicate = getPredicate(screen);
        Page<LegalWalletRecharge> page = legalWalletRechargeService.findAll(predicate, pageModel);
        return success(page);
    }

    @GetMapping("{id}")
    public MessageResult id(@PathVariable("id") Long id) {
        LegalWalletRecharge legalWalletRecharge = legalWalletRechargeService.findOne(id);
        Assert.notNull(legalWalletRecharge, "validate id!");
        return success(legalWalletRecharge);
    }

    //充值通过
    @PatchMapping("{id}/pass")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult pass(@PathVariable("id") Long id) {
        //充值校验
        LegalWalletRecharge legalWalletRecharge = legalWalletRechargeService.findOne(id);
        MemberTransaction memberTransaction = new MemberTransaction() ;
        memberTransaction.setAmount(legalWalletRecharge.getAmount());
        memberTransaction.setAddress("");
        memberTransaction.setMemberId(legalWalletRecharge.getMember().getId());
        memberTransaction.setFee(BigDecimal.ZERO);
        memberTransaction.setSymbol(legalWalletRecharge.getCoin().getUnit());
        memberTransaction.setType(TransactionType.LEGAL_RECHARGE);
        memberTransactionService.save(memberTransaction);

        Assert.notNull(legalWalletRecharge, "validate id!");
        Assert.isTrue(legalWalletRecharge.getState() == LegalWalletState.APPLYING, "申请已经结束!");
        //校验钱包
        MemberWallet wallet = walletService.findByCoinAndMember(legalWalletRecharge.getCoin(), legalWalletRecharge.getMember());
        org.springframework.util.Assert.notNull(wallet, "wallet null!");
        //充值请求通过业务
        legalWalletRechargeService.pass(wallet, legalWalletRecharge);
        return success();
    }

    //虚假充值
    @PatchMapping("{id}/no-pass")
    public MessageResult noPass(@PathVariable("id") Long id) {
        LegalWalletRecharge legalWalletRecharge = legalWalletRechargeService.findOne(id);
        Assert.notNull(legalWalletRecharge, "validate id!");
        Assert.isTrue(legalWalletRecharge.getState() == LegalWalletState.APPLYING, "申请已经结束!");
        legalWalletRechargeService.noPass(legalWalletRecharge);
        return success();
    }

    //条件
    private Predicate getPredicate(LegalWalletRechargeScreen screen) {
        ArrayList<BooleanExpression> booleanExpressions = new ArrayList<>();
        if (StringUtils.isNotBlank(screen.getUsername()))
            booleanExpressions.add(QLegalWalletRecharge.legalWalletRecharge.member.username.eq(screen.getUsername()));
        if (screen.getStatus() != null)
            booleanExpressions.add(QLegalWalletRecharge.legalWalletRecharge.state.eq(screen.getStatus()));
        if (StringUtils.isNotBlank(screen.getCoinName()))
            booleanExpressions.add(QLegalWalletRecharge.legalWalletRecharge.coin.name.eq(screen.getCoinName()));
        return PredicateUtils.getPredicate(booleanExpressions);
    }
}
