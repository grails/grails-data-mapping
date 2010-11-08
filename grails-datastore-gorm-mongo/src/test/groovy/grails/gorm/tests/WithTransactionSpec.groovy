package grails.gorm.tests

import spock.lang.Ignore;

class WithTransactionSpec extends GormDatastoreSpec{

  @Ignore
  void "Test save() with transaction"() {
  		// Mongo doesn't support transactions
  }


  @Ignore
  void "Test rollback transaction"() {
  		// Mongo doesn't support transactions
  }

  @Ignore
  void "Test rollback transaction with Exception"() {
  		// Mongo doesn't support transactions
  }
}
