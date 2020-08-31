package org.radarbase.authorizer.doa

import org.radarbase.authorizer.logger
import org.radarbase.jersey.exception.HttpInternalServerException
import java.io.Closeable
import javax.persistence.EntityManager
import javax.persistence.EntityTransaction

/**
 * Run a transaction and commit it. If an exception occurs, the transaction is rolled back.
 */
fun <T> EntityManager.transact(transactionOperation: EntityManager.() -> T) = createTransaction {
  it.use { transactionOperation() }
}

/**
 * Start a transaction without committing it. If an exception occurs, the transaction is rolled back.
 */
private fun <T> EntityManager.createTransaction(transactionOperation: EntityManager.(CloseableTransaction) -> T): T {
  val currentTransaction = transaction
      ?: throw HttpInternalServerException("transaction_not_found", "Cannot find a transaction from EntityManager")

  currentTransaction.begin()
  try {
    return transactionOperation(object : CloseableTransaction {
      override val transaction: EntityTransaction = currentTransaction

      override fun close() {
        try {
          transaction.commit()
        } catch (ex: Exception) {
          logger.error("Rolling back operation", ex)
          if (currentTransaction.isActive) {
            currentTransaction.rollback()
          }
          throw ex
        }
      }
    })
  } catch (ex: Exception) {
    logger.error("Rolling back operation", ex)
    if (currentTransaction.isActive) {
      currentTransaction.rollback()
    }
    throw ex
  }
}


interface CloseableTransaction : Closeable {
  val transaction: EntityTransaction
  override fun close()
}
