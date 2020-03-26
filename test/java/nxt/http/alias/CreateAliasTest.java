/*
 * Copyright © 2016-2019 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.http.alias;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.http.APICall;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import static nxt.blockchain.ChildChain.IGNIS;

public class CreateAliasTest extends BlockchainTest {
    @Test
    public void testAliasLen258() {
        String char3Byte = "€";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 86; i++) {
            sb.append(char3Byte);
        }
        String name = sb.toString();
        String uri = "nxt://test " + name + name;
        APICall.Builder builder = new APICall.Builder("setAlias").
                param("publicKey", ALICE.getPublicKeyStr()).param("chain", IGNIS.getId()).feeNQT(IGNIS.ONE_COIN * 20).
                param("broadcast", "false").
                param("aliasName", "153307605").param("aliasURI", uri);
        JSONObject response = builder.build().invoke();

        JSONObject unsignedTransactionJSON = (JSONObject) response.get("transactionJSON");
        JSONObject attachment = (JSONObject) unsignedTransactionJSON.get("attachment");
        attachment.replace("alias", name);

        JSONObject signResult = new APICall.Builder("signTransaction").
                param("secretPhrase", ALICE.getSecretPhrase()).
                param("unsignedTransactionJSON", unsignedTransactionJSON.toJSONString()).build().invoke();

        Assert.assertEquals(4L, signResult.get("errorCode"));
        BlockchainTest.generateBlock();

        String fixedName = "153307605";
        attachment.replace("alias", fixedName);

        signResult = new APICall.Builder("signTransaction").
                param("secretPhrase", ALICE.getSecretPhrase()).
                param("unsignedTransactionJSON", unsignedTransactionJSON.toJSONString()).build().invoke();

        response = new APICall.Builder("broadcastTransaction").
                param("transactionBytes", ((String)signResult.get("transactionBytes"))).
                build().invoke();
        Logger.logDebugMessage("broadcastTransaction: " + response);
        generateBlock();

        response = new APICall.Builder("getAlias").param("aliasName", fixedName).build().invoke();
        Assert.assertEquals(uri, response.get("aliasURI"));
    }
}
