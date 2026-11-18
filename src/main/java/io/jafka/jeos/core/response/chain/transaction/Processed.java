package io.jafka.jeos.core.response.chain.transaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jafka.jeos.core.common.ActionTrace;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Processed {

    private List<ActionTrace> actionTraces;
    private Integer elapsed;
    private String except;
    private String id;
    private Long netUsage;
    private Receipt receipt;
    private Boolean scheduled;
    private Long blockNum;
    private String blockTime;

/*    
    private List<String> deferredTransactionRequests;

    private String cpuUsage;

    private String status;

    private List<Lock> writeLocks;

    private String regionId;

    private String netUsageWords;

    private String sharedIndex;

    

    private String cycleIndex;

    private String setupProfilingUs;

    private String profilingUs;


    private String packedTrxDigest;

    private List<Lock> readLocks;*/

}
