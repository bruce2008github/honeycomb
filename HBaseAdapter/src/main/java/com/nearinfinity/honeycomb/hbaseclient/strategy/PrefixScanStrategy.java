package com.nearinfinity.honeycomb.hbaseclient.strategy;

import com.nearinfinity.honeycomb.hbaseclient.*;
import org.apache.hadoop.hbase.client.Scan;

import java.util.Map;

public class PrefixScanStrategy implements ScanStrategy {
    private final ScanStrategyInfo scanInfo;

    public PrefixScanStrategy(ScanStrategyInfo scanInfo) {
        this.scanInfo = scanInfo;
    }

    @Override
    public Scan getScan(TableInfo info) {
        long tableId = info.getId();
        Map<String, byte[]> ascendingValueMap = ValueEncoder.correctAscendingValuePadding(info, this.scanInfo.keyValueMap(), this.scanInfo.nullSearchColumns());
        byte[] columnId = Index.createColumnIds(this.scanInfo.columnNames(), info.columnNameToIdMap());
        byte[] paddedValue = Index.convertToHBaseFormat(this.scanInfo.keyValueColumns(), ascendingValueMap);

        byte[] startKey = RowKeyFactory.buildIndexRowKey(tableId, columnId, paddedValue, Constants.ZERO_UUID);
        byte[] endKey = RowKeyFactory.buildIndexRowKey(tableId, columnId, paddedValue, Constants.FULL_UUID);

        Scan scan = ScanFactory.buildScan(startKey, endKey);

        return scan;
    }

    @Override
    public String getTableName() {
        return this.scanInfo.tableName();
    }
}
