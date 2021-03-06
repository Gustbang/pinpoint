/*
 * Copyright 2017 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.common.server.bo.codec.stat.v2;

import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.server.bo.codec.stat.AgentStatCodec;
import com.navercorp.pinpoint.common.server.bo.codec.stat.AgentStatDataPointCodec;
import com.navercorp.pinpoint.common.server.bo.codec.stat.header.AgentStatHeaderDecoder;
import com.navercorp.pinpoint.common.server.bo.codec.stat.header.AgentStatHeaderEncoder;
import com.navercorp.pinpoint.common.server.bo.codec.stat.header.BitCountingHeaderEncoder;
import com.navercorp.pinpoint.common.server.bo.codec.stat.strategy.StrategyAnalyzer;
import com.navercorp.pinpoint.common.server.bo.codec.stat.strategy.UnsignedLongEncodingStrategy;
import com.navercorp.pinpoint.common.server.bo.codec.strategy.EncodingStrategy;
import com.navercorp.pinpoint.common.server.bo.stat.ResponseTimeBo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Taejin Koo
 */
@Component("responseTimeCodecV2")
public class ResponseTimeCodecV2 extends AbstractAgentStatCodecV2<ResponseTimeBo> {

    @Autowired
    public ResponseTimeCodecV2(AgentStatDataPointCodec codec) {
        super(codec);
    }

    @Override
    protected CodecEncoder createCodecEncoder() {
        return new ResponseTimeCodecEncoder(codec);
    }

    @Override
    protected CodecDecoder createCodecDecoder() {
        return new ResponseTimeCodecDecoder(codec);
    }

    private static class ResponseTimeCodecEncoder implements AgentStatCodec.CodecEncoder<ResponseTimeBo> {

        private final AgentStatDataPointCodec codec;
        private final UnsignedLongEncodingStrategy.Analyzer.Builder avgAnalyzerBuilder = new UnsignedLongEncodingStrategy.Analyzer.Builder();

        public ResponseTimeCodecEncoder(AgentStatDataPointCodec codec) {
            this.codec = codec;
        }

        @Override
        public void addValue(ResponseTimeBo agentStatDataPoint) {
            avgAnalyzerBuilder.addValue(agentStatDataPoint.getAvg());
        }

        @Override
        public void encode(Buffer valueBuffer) {
            StrategyAnalyzer<Long> avgStrategyAnalyzer = avgAnalyzerBuilder.build();

            // encode header
            AgentStatHeaderEncoder headerEncoder = new BitCountingHeaderEncoder();
            headerEncoder.addCode(avgStrategyAnalyzer.getBestStrategy().getCode());

            final byte[] header = headerEncoder.getHeader();
            valueBuffer.putPrefixedBytes(header);
            // encode values
            codec.encodeValues(valueBuffer, avgStrategyAnalyzer.getBestStrategy(), avgStrategyAnalyzer.getValues());
        }

    }

    private static class ResponseTimeCodecDecoder implements AgentStatCodec.CodecDecoder<ResponseTimeBo> {

        private final AgentStatDataPointCodec codec;
        private List<Long> avgs;

        public ResponseTimeCodecDecoder(AgentStatDataPointCodec codec) {
            this.codec = codec;
        }

        @Override
        public void decode(Buffer valueBuffer, AgentStatHeaderDecoder headerDecoder, int valueSize) {
            EncodingStrategy<Long> avgEncodingStrategy = UnsignedLongEncodingStrategy.getFromCode(headerDecoder.getCode());
            this.avgs = codec.decodeValues(valueBuffer, avgEncodingStrategy, valueSize);
        }

        @Override
        public ResponseTimeBo getValue(int index) {
            ResponseTimeBo responseTimeBo = new ResponseTimeBo();
            responseTimeBo.setAvg(avgs.get(index));
            return responseTimeBo;
        }

    }

}
