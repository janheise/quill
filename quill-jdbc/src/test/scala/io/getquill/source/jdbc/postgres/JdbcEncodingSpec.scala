package io.getquill.source.jdbc.postgres

import io.getquill._
import io.getquill.source.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    testPostgresDB.run(delete)
    testPostgresDB.run(insert).using(insertValues)
    verify(testPostgresDB.run(query[EncodingTestEntity]))
  }
}
