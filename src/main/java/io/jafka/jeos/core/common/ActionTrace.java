package io.jafka.jeos.core.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * One executed action. Since EOSIO 2.1 an action may declare a return value, reported as
 * {@code return_value_hex_data} and, when the ABI is known, {@code return_value_data}
 * (upstream issue #22).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionTrace {

    private Action act;

    private String console;

    private Integer cpuUsage;

    private Integer elapsed;

    private String receiver;

    @JsonDeserialize(using = InlineTracesDeserializer.class)
    private List<Object> inlineTraces = null;

    private Receipt receipt;

    private Integer totalCpuUsage;

    private String trxId;

    private Long blockNum;

    private String blockTime;

    /** Raw return value of the action, hex encoded (EOSIO 2.1+). */
    private String returnValueHexData;

    /** Decoded return value of the action, when the node knows the contract ABI (EOSIO 2.1+). */
    private Object returnValueData;

    public Action getAct() {
        return act;
    }

    @JsonProperty("act")
    public void setAct(Action act) {
        this.act = act;
    }

    public String getConsole() {
        return console;
    }

    @JsonProperty("console")
    public void setConsole(String console) {
        this.console = console;
    }

    public Integer getCpuUsage() {
        return cpuUsage;
    }

    @JsonProperty("cpu_usage")
    public void setCpuUsage(Integer cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Integer getElapsed() {
        return elapsed;
    }

    @JsonProperty("elapsed")
    public void setElapsed(Integer elapsed) {
        this.elapsed = elapsed;
    }

    public String getReceiver() {
        return receiver;
    }

    @JsonProperty("receiver")
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public List<Object> getInlineTraces() {
        return inlineTraces;
    }

    @JsonProperty("inline_traces")
    public void setInlineTraces(List<Object> inlineTraces) {
        this.inlineTraces = inlineTraces;
    }

    public Receipt getReceipt() {
        return receipt;
    }

    @JsonProperty("receipt")
    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    public Integer getTotalCpuUsage() {
        return totalCpuUsage;
    }

    @JsonProperty("total_cpu_usage")
    public void setTotalCpuUsage(Integer totalCpuUsage) {
        this.totalCpuUsage = totalCpuUsage;
    }

    public String getTrxId() {
        return trxId;
    }

    @JsonProperty("trx_id")
    public void setTrxId(String trxId) {
        this.trxId = trxId;
    }

    public Long getBlockNum() {
        return blockNum;
    }

    @JsonProperty("block_num")
    public void setBlockNum(Long blockNum) {
        this.blockNum = blockNum;
    }

    public String getBlockTime() {
        return blockTime;
    }

    @JsonProperty("block_time")
    public void setBlockTime(String blockTime) {
        this.blockTime = blockTime;
    }

    public String getReturnValueHexData() {
        return returnValueHexData;
    }

    @JsonProperty("return_value_hex_data")
    public void setReturnValueHexData(String returnValueHexData) {
        this.returnValueHexData = returnValueHexData;
    }

    public Object getReturnValueData() {
        return returnValueData;
    }

    @JsonProperty("return_value_data")
    public void setReturnValueData(Object returnValueData) {
        this.returnValueData = returnValueData;
    }
}