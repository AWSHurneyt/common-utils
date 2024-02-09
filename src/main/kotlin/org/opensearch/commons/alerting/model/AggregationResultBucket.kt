package org.opensearch.commons.alerting.model

import org.apache.logging.log4j.LogManager
import org.opensearch.core.common.ParsingException
import org.opensearch.core.common.io.stream.StreamInput
import org.opensearch.core.common.io.stream.StreamOutput
import org.opensearch.core.common.io.stream.Writeable
import org.opensearch.core.xcontent.ToXContent
import org.opensearch.core.xcontent.ToXContentObject
import org.opensearch.core.xcontent.XContentBuilder
import org.opensearch.core.xcontent.XContentParser
import org.opensearch.core.xcontent.XContentParser.Token
import org.opensearch.core.xcontent.XContentParserUtils.ensureExpectedToken
import java.io.IOException
import java.util.Locale

private val logger = LogManager.getLogger(AggregationResultBucket::class.java)

data class AggregationResultBucket(
    val parentBucketPath: String?,
    val bucketKeys: List<String>,
    val bucket: Map<String, Any>? // TODO: Should reduce contents to only top-level to not include sub-aggs here
) : Writeable, ToXContentObject {

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(sin.readString(), sin.readStringList(), sin.readMap())

    override fun writeTo(out: StreamOutput) {
        out.writeString(parentBucketPath)
        out.writeStringCollection(bucketKeys)
        out.writeMap(bucket)
    }

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        builder.startObject()
        innerXContent(builder)
        return builder.endObject()
    }

    fun innerXContent(builder: XContentBuilder): XContentBuilder {
        logger.info("hurneyt innerXContent::PARENTS_BUCKET_PATH = {}", parentBucketPath)
        logger.info("hurneyt innerXContent::BUCKET_KEYS = {}", bucketKeys.toTypedArray())
        logger.info("hurneyt innerXContent::BUCKET = {}", bucket)
        builder.startObject(CONFIG_NAME)
            .field(PARENTS_BUCKET_PATH, parentBucketPath)
            .field(BUCKET_KEYS, bucketKeys.toTypedArray())
            .field(BUCKET, bucket)
            .endObject()
        return builder
    }

    fun asTemplateArg(): Map<String, Any?> {
        logger.info("hurneyt asTemplateArg START")
        logger.info("hurneyt asTemplateArg::PARENTS_BUCKET_PATH = {}", parentBucketPath)
        logger.info("hurneyt asTemplateArg::BUCKET_KEYS = {}", bucketKeys.toTypedArray())
        logger.info("hurneyt asTemplateArg::BUCKET = {}", bucket)
        val output = mapOf(
            PARENTS_BUCKET_PATH to parentBucketPath,
            BUCKET_KEYS to bucketKeys,
            BUCKET to bucket
        )
        logger.info("hurneyt asTemplateArg END")
        return output
    }

    companion object {
        const val CONFIG_NAME = "agg_alert_content"
        const val PARENTS_BUCKET_PATH = "parent_bucket_path"
        const val BUCKET_KEYS = "bucket_keys"
        private const val BUCKET = "bucket"

        fun parse(xcp: XContentParser): AggregationResultBucket {
            var parentBucketPath: String? = null
            var bucketKeys = mutableListOf<String>()
            var bucket: MutableMap<String, Any>? = null
            ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.currentToken(), xcp)

            if (CONFIG_NAME != xcp.currentName()) {
                throw ParsingException(
                    xcp.tokenLocation,
                    String.format(
                        Locale.ROOT,
                        "Failed to parse object: expecting token with name [%s] but found [%s]",
                        CONFIG_NAME,
                        xcp.currentName()
                    )
                )
            }
            while (xcp.nextToken() != Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()
                when (fieldName) {
                    PARENTS_BUCKET_PATH -> parentBucketPath = xcp.text()
                    BUCKET_KEYS -> {
                        ensureExpectedToken(Token.START_ARRAY, xcp.currentToken(), xcp)
                        while (xcp.nextToken() != Token.END_ARRAY) {
                            bucketKeys.add(xcp.text())
                        }
                    }
                    BUCKET -> bucket = xcp.map()
                }
            }
            return AggregationResultBucket(parentBucketPath, bucketKeys, bucket)
        }
    }
}
