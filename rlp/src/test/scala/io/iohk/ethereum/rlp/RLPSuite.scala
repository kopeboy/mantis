package io.iohk.ethereum.rlp

import akka.util.ByteString
import io.iohk.ethereum.rlp.RLPImplicitConversions._
import io.iohk.ethereum.rlp.RLPImplicits._
import io.iohk.ethereum.utils.Hex
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.{ScalaCheckDrivenPropertyChecks, ScalaCheckPropertyChecks}
import scala.language.implicitConversions
import scala.util.Try
import org.scalatest.funsuite.AnyFunSuite

class RLPSuite extends AnyFunSuite with ScalaCheckPropertyChecks with ScalaCheckDrivenPropertyChecks {

  test("nextElementIndex of empty data") {
    val maybeIndex = Try { nextElementIndex(Array.emptyByteArray, 0) }
    assert(maybeIndex.isFailure)
  }

  test("Decoding of empty data") {
    val maybeDecoded = Try { decode[Array[Byte]](Array.emptyByteArray) }
    assert(maybeDecoded.isFailure)
  }

  test("Decoding failure: Passing RLPValue when RLPList is expected") {
    val data = encode(0.toLong)
    val maybeSeqObtained = Try { decode[Seq[Long]](data)(seqEncDec()) }
    assert(maybeSeqObtained.isFailure)
  }

  test("Decoding failure: Passing RLPList when RLPValue is expected") {
    val data = RLP.encode(RLPList("cat", "dog"))
    val maybeByteObtained = Try { decode[Byte](data) }
    val maybeShortObtained = Try { decode[Short](data) }
    val maybeIntObtained = Try { decode[Int](data) }
    val maybeLongObtained = Try { decode[Long](data) }
    val maybeBigIntObtained = Try { decode[BigInt](data) }
    val maybeStringObtained = Try { decode[String](data) }
    val maybeByteArrayObtained = Try { decode[Array[Byte]](data) }
    assert(maybeByteObtained.isFailure)
    assert(maybeShortObtained.isFailure)
    assert(maybeIntObtained.isFailure)
    assert(maybeLongObtained.isFailure)
    assert(maybeStringObtained.isFailure)
    assert(maybeByteArrayObtained.isFailure)
    assert(maybeBigIntObtained.isFailure)
  }

  test("Decoding failure: Passing an RLPValue larger than expected") {
    val num: BigInt = 16 * BigInt(Long.MaxValue)
    val data = encode(num)
    val maybeByteObtained = Try { decode[Byte](data) }
    val maybeShortObtained = Try { decode[Short](data) }
    val maybeIntObtained = Try { decode[Int](data) }
    val maybeLongObtained = Try { decode[Long](data) }
    assert(maybeByteObtained.isFailure)
    assert(maybeShortObtained.isFailure)
    assert(maybeIntObtained.isFailure)
    assert(maybeLongObtained.isFailure)
  }

  test("Byte Encoding") {
    val expected = Array[Byte](0x80.toByte)
    val data = encode(0: Byte)

    assert(expected sameElements data)
    val dataObtained = decode[Byte](data)
    val obtained: Byte = dataObtained
    assert((0: Byte) == obtained)

    val expected2 = Array[Byte](0x78.toByte)
    val data2 = encode(120: Byte)
    assert(expected2 sameElements data2)
    val dataObtained2 = decode[Byte](data2)
    val obtained2: Byte = dataObtained2
    assert((120: Byte) == obtained2)

    val expected3 = Array[Byte](0x7f.toByte)
    val data3 = encode(127: Byte)
    assert(expected3 sameElements data3)
    val dataObtained3 = decode[Byte](data3)
    val obtained3: Byte = dataObtained3
    assert((127: Byte) == obtained3)

    forAll(Gen.choose[Byte](Byte.MinValue, Byte.MaxValue)) { (aByte: Byte) =>
      {
        val data = encode(aByte)
        val dataObtained = decode[Byte](data)
        val obtained: Byte = dataObtained
        assert(aByte == obtained)
      }
    }
  }

  test("Short Encoding") {
    val expected4 = Array[Byte](0x82.toByte, 0x76.toByte, 0x5f.toByte)
    val data4 = encode(30303.toShort)
    assert(expected4 sameElements data4)
    val dataObtained4 = decode[Short](data4)
    val obtained4: Short = dataObtained4
    assert((30303: Short) == obtained4)

    val expected5 = Array[Byte](0x82.toByte, 0x4e.toByte, 0xea.toByte)
    val data5 = encode(20202.toShort)
    assert(expected5 sameElements data5)
    val dataObtained5 = decode[Short](data5)
    val obtained5: Short = dataObtained5
    assert((20202: Short) == obtained5)

    val expected6 = Array[Byte](0x82.toByte, 0x9d.toByte, 0x0a.toByte)
    val data6 = encode(40202.toShort)
    assert(expected6 sameElements data6)
    val dataObtained6 = decode[Short](data6)
    val obtained6: Short = dataObtained6
    assert(40202.toShort == obtained6)

    val expected7 = Array[Byte](0x7f.toByte)
    val data7 = encode(127.toShort)
    assert(expected7 sameElements data7)
    val dataObtained7 = decode[Short](data7)
    val obtained7: Short = dataObtained7
    assert(127.toShort == obtained7)

    val expected8 = Array[Byte](0x80.toByte)
    val data8 = encode(0.toShort)
    assert(expected8 sameElements data8)
    val dataObtained8 = decode[Short](data8)
    val obtained8: Short = dataObtained8
    assert(0.toShort == obtained8)

    forAll(Gen.choose[Short](Short.MinValue, Short.MaxValue)) { (aShort: Short) =>
      {
        val data = encode(aShort)
        val dataObtained = decode[Short](data)
        val obtained: Short = dataObtained
        assert(aShort == obtained)
      }
    }
  }

  test("String encoding") {
    val expected = Array[Byte](0x80.toByte)
    val data = encode("")
    assert(expected sameElements data)
    val dataObtained = decode[String](data)
    val obtained: String = dataObtained
    assert("" == obtained)

    val expected2 = Array[Byte](
      0x90.toByte,
      0x45.toByte,
      0x74.toByte,
      0x68.toByte,
      0x65.toByte,
      0x72.toByte,
      0x65.toByte,
      0x75.toByte,
      0x6d.toByte,
      0x4a.toByte,
      0x20.toByte,
      0x43.toByte,
      0x6c.toByte,
      0x69.toByte,
      0x65.toByte,
      0x6e.toByte,
      0x74.toByte
    )
    val data2 = encode("EthereumJ Client")
    assert(expected2 sameElements data2)
    val dataObtained2 = decode[String](data2)
    val obtained2: String = dataObtained2
    assert("EthereumJ Client" == obtained2)

    val expected3 = Array[Byte](
      0xad.toByte,
      0x45.toByte,
      0x74.toByte,
      0x68.toByte,
      0x65.toByte,
      0x72.toByte,
      0x65.toByte,
      0x75.toByte,
      0x6d.toByte,
      0x28.toByte,
      0x2b.toByte,
      0x2b.toByte,
      0x29.toByte,
      0x2f.toByte,
      0x5a.toByte,
      0x65.toByte,
      0x72.toByte,
      0x6f.toByte,
      0x47.toByte,
      0x6f.toByte,
      0x78.toByte,
      0x2f.toByte,
      0x76.toByte,
      0x30.toByte,
      0x2e.toByte,
      0x35.toByte,
      0x2e.toByte,
      0x30.toByte,
      0x2f.toByte,
      0x6e.toByte,
      0x63.toByte,
      0x75.toByte,
      0x72.toByte,
      0x73.toByte,
      0x65.toByte,
      0x73.toByte,
      0x2f.toByte,
      0x4c.toByte,
      0x69.toByte,
      0x6e.toByte,
      0x75.toByte,
      0x78.toByte,
      0x2f.toByte,
      0x67.toByte,
      0x2b.toByte,
      0x2b.toByte
    )
    val data3 = encode("Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++")
    assert(expected3 sameElements data3)
    val dataObtained3 = decode[String](data3)
    val obtained3: String = dataObtained3
    assert("Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++" == obtained3)

    val expected4 = Array[Byte](
      0xb8.toByte,
      0x5a.toByte,
      0x45.toByte,
      0x74.toByte,
      0x68.toByte,
      0x65.toByte,
      0x72.toByte,
      0x65.toByte,
      0x75.toByte,
      0x6d.toByte,
      0x28.toByte,
      0x2b.toByte,
      0x2b.toByte,
      0x29.toByte,
      0x2f.toByte,
      0x5a.toByte,
      0x65.toByte,
      0x72.toByte,
      0x6f.toByte,
      0x47.toByte,
      0x6f.toByte,
      0x78.toByte,
      0x2f.toByte,
      0x76.toByte,
      0x30.toByte,
      0x2e.toByte,
      0x35.toByte,
      0x2e.toByte,
      0x30.toByte,
      0x2f.toByte,
      0x6e.toByte,
      0x63.toByte,
      0x75.toByte,
      0x72.toByte,
      0x73.toByte,
      0x65.toByte,
      0x73.toByte,
      0x2f.toByte,
      0x4c.toByte,
      0x69.toByte,
      0x6e.toByte,
      0x75.toByte,
      0x78.toByte,
      0x2f.toByte,
      0x67.toByte,
      0x2b.toByte,
      0x2b.toByte,
      0x45.toByte,
      0x74.toByte,
      0x68.toByte,
      0x65.toByte,
      0x72.toByte,
      0x65.toByte,
      0x75.toByte,
      0x6d.toByte,
      0x28.toByte,
      0x2b.toByte,
      0x2b.toByte,
      0x29.toByte,
      0x2f.toByte,
      0x5a.toByte,
      0x65.toByte,
      0x72.toByte,
      0x6f.toByte,
      0x47.toByte,
      0x6f.toByte,
      0x78.toByte,
      0x2f.toByte,
      0x76.toByte,
      0x30.toByte,
      0x2e.toByte,
      0x35.toByte,
      0x2e.toByte,
      0x30.toByte,
      0x2f.toByte,
      0x6e.toByte,
      0x63.toByte,
      0x75.toByte,
      0x72.toByte,
      0x73.toByte,
      0x65.toByte,
      0x73.toByte,
      0x2f.toByte,
      0x4c.toByte,
      0x69.toByte,
      0x6e.toByte,
      0x75.toByte,
      0x78.toByte,
      0x2f.toByte,
      0x67.toByte,
      0x2b.toByte,
      0x2b.toByte
    )
    val data4 = encode("Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++")
    assert(expected4 sameElements data4)
    val dataObtained4 = decode[String](data4)
    val obtained4: String = dataObtained4
    assert("Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++" == obtained4)

    val strGen = (n: Int) => Gen.choose(0, n).flatMap(long => Gen.listOfN(long, Gen.alphaChar).map(_.mkString))

    forAll(strGen(10000)) { (aString: String) =>
      {
        val data = encode(aString)
        val dataObtained = decode[String](data)
        val obtained: String = dataObtained
        assert(aString == obtained)
      }
    }
  }

  test("Int Encoding") {
    val expected = Array[Byte](0x80.toByte)
    val data = encode(0)
    assert(expected sameElements data)
    val dataObtained = decode[Int](data)
    val obtained: Int = dataObtained
    assert(0 == obtained)

    val expected2 = Array(0x78.toByte)
    val data2 = encode(120)
    assert(expected2 sameElements data2)
    val dataObtained2 = decode[Int](data2)
    val obtained2: Int = dataObtained2
    assert(120 == obtained2)

    val expected3 = Array(0x7f.toByte)
    val data3 = encode(127)
    assert(expected3 sameElements data3)
    val dataObtained3 = decode[Int](data3)
    val obtained3: Int = dataObtained3
    assert(127 == obtained3)

    val expected4 = Array(0x82.toByte, 0x76.toByte, 0x5f.toByte)
    val data4 = encode(30303)
    assert(expected4 sameElements data4)
    val dataObtained4 = decode[Int](data4)
    val obtained4: Int = dataObtained4
    assert(30303 == obtained4)

    val expected5 = Array(0x82.toByte, 0x4e.toByte, 0xea.toByte)
    val data5 = encode(20202)
    assert(expected5 sameElements data5)
    val dataObtained5 = decode[Int](data5)
    val obtained5: Int = dataObtained5
    assert(20202 == obtained5)

    val expected6 = Array(0x83.toByte, 1.toByte, 0.toByte, 0.toByte)
    val data6 = encode(65536)
    assert(expected6 sameElements data6)
    val dataObtained6 = decode[Int](data6)
    val obtained6: Int = dataObtained6
    assert(65536 == obtained6)

    val expected7 = Array(0x84.toByte, 0x80.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte)
    val data7 = encode(Integer.MIN_VALUE)
    assert(expected7 sameElements data7)
    val dataObtained7 = decode[Int](data7)
    val obtained7: Int = dataObtained7
    assert(Integer.MIN_VALUE == obtained7)

    val expected8 = Array(0x84.toByte, 0x7f.toByte, 0xff.toByte, 0xff.toByte, 0xff.toByte)
    val data8 = encode(Integer.MAX_VALUE)
    assert(expected8 sameElements data8)
    val dataObtained8 = decode[Int](data8)
    val obtained8: Int = dataObtained8
    assert(Integer.MAX_VALUE == obtained8)

    val expected9 = Array(0x84.toByte, 0xff.toByte, 0xff.toByte, 0xff.toByte, 0xff.toByte)
    val data9 = encode(0xffffffff)
    assert(expected9 sameElements data9)
    val dataObtained9 = decode[Int](data9)
    val obtained9: Int = dataObtained9
    assert(0xffffffff == obtained9)

    forAll(Gen.choose[Int](Int.MinValue, Int.MaxValue)) { (anInt: Int) =>
      {
        val data = encode(anInt)
        val dataObtained = decode[Int](data)
        val obtained: Int = dataObtained
        assert(anInt == obtained)
      }
    }
  }

  test("Long Encoding") {
    forAll(Gen.choose[Long](0, Long.MaxValue)) { (aLong: Long) =>
      {
        val data = encode(aLong)
        val dataObtained = decode[Long](data)
        val obtained: Long = dataObtained
        assert(aLong == obtained)
      }
    }
  }

  test("BigInt Encoding") {
    val expected = Array[Byte](0x80.toByte)
    val data = encode(BigInt(0))
    assert(expected sameElements data)
    val dataObtained = decode[BigInt](data)
    val obtained: BigInt = dataObtained
    assert(BigInt(0) == obtained)

    val bigInt = BigInt("100102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", 16)
    val expected2 = "a0100102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
    val data2 = encode(bigInt)
    assert(expected2 equals Hex.toHexString(data2))
    val dataObtained2 = decode[BigInt](data2)
    val obtained2: BigInt = dataObtained2
    assert(bigInt == obtained2)

    forAll(Arbitrary.arbitrary[BigInt]) { (aBigIntSigned: BigInt) =>
      {
        val aBigInt = aBigIntSigned.abs
        val data = encode(aBigInt)
        val dataObtained = decode[BigInt](data)
        val obtained: BigInt = dataObtained
        assert(aBigInt == obtained)
      }
    }
  }

  test("Byte Array Encoding") {
    val byteArr =
      "ce73660a06626c1b3fda7b18ef7ba3ce17b6bf604f9541d3c6c654b7ae88b239407f659c78f419025d785727ed017b6add21952d7e12007373e321dbc31824ba"
    val byteArray: Array[Byte] = Hex.decode(byteArr)
    val expected = "b840" + byteArr

    val data = encode(byteArray)
    assert(expected equals Hex.toHexString(data))
    val dataObtained = decode[Array[Byte]](data)
    val obtained: Array[Byte] = dataObtained
    assert(byteArray sameElements obtained)

    val shouldBeError = Try {
      val byteArray255Elements = Array.fill(255)(0x1.toByte)
      encode(byteArray255Elements)
    }
    assert(shouldBeError.isSuccess)

    forAll(Gen.nonEmptyListOf(Arbitrary.arbitrary[Byte])) { (aByteList: List[Byte]) =>
      {
        val data = encode(aByteList.toArray)
        val dataObtained = decode[Array[Byte]](data)
        val obtained: Array[Byte] = dataObtained
        assert(aByteList.toArray sameElements obtained)
      }
    }
  }

  test("Encode ByteString") {
    forAll(Gen.nonEmptyListOf(Arbitrary.arbitrary[Byte])) { (aByteList: List[Byte]) =>
      {
        val byteString = ByteString(aByteList.toArray)
        val data = encode(byteString)
        val dataObtained = decode[ByteString](data)
        val obtained: ByteString = dataObtained
        assert(byteString equals obtained)
      }
    }
  }

  test("Encode Seq") {
    forAll(Gen.nonEmptyListOf(Gen.choose[Long](0, Long.MaxValue))) { (aLongList: List[Long]) =>
      {
        val aLongSeq: Seq[Long] = aLongList
        val data = encode(aLongSeq)(seqEncDec())
        val dataObtained: Seq[Long] = decode[Seq[Long]](data)(seqEncDec())
        assert(aLongSeq equals dataObtained)
      }
    }
  }

  test("Encode Empty List") {
    val expected = "c0"
    val data = encode(Seq[Any]())
    assert(expected == Hex.toHexString(data))

    val dataObtained = decode[Seq[Any]](data)
    val obtained: Seq[Any] = dataObtained
    assert(obtained.isEmpty)
  }

  test("Encode Short  List") {
    val expected = "c88363617483646f67"
    val data = RLP.encode(RLPList("cat", "dog"))
    assert(expected == Hex.toHexString(data))
    val dataObtained = decode[Seq[String]](data)(stringSeqEncDec)
    val obtained = dataObtained
    assert(Seq("cat", "dog") equals obtained)

    val expected2 = "cc83646f6783676f6483636174"
    val data2 = RLP.encode(RLPList("dog", "god", "cat"))
    assert(expected2 == Hex.toHexString(data2))
    val dataObtained2 = decode[Seq[String]](data2)(stringSeqEncDec)
    val obtained2 = dataObtained2
    assert(Seq("dog", "god", "cat") equals obtained2)
  }

  test("Encode Long  List") {
    val list = Seq("cat", "Lorem ipsum dolor sit amet, consectetur adipisicing elit")
    val expected =
      "f83e83636174b8384c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c6974"
    val data = RLP.encode(RLPList(list.map(i => toEncodeable(i)): _*))
    assert(expected == Hex.toHexString(data))
    val dataObtained = decode[Seq[String]](data)(stringSeqEncDec)
    val obtained = dataObtained
    assert(list equals obtained)
  }

  test("Encode multilist") {
    val expected = "cc01c48363617483646f67c102"
    val multilist1 = MultiList1(1, Seq("cat"), "dog", Seq(2))
    val data = encode(multilist1)(MultiList1.encDec)
    assert(expected == Hex.toHexString(data))
    val dataObtained = decode[MultiList1](data)
    val obtained = dataObtained
    assert(multilist1 equals obtained)

    val multilist2 = MultiList2(Seq("cat", "dog"), Seq(1, 2))
    val expected2 = "cdc88363617483646f67c20102c0"
    val data2 = encode(multilist2)(MultiList2.encDec)
    assert(expected2 == Hex.toHexString(data2))
    val dataObtained2 = decode[MultiList2](data2)
    val obtained2 = dataObtained2
    assert(multilist2 equals obtained2)
  }

  test("Encode Empty List Of List") {
    val emptyListOfList = EmptyListOfList()
    val expected = "c4c2c0c0c0"
    val data = encode(emptyListOfList)(EmptyListOfList.encDec)
    assert(expected == Hex.toHexString(data))
    val dataObtained = decode[EmptyListOfList](data)
    val obtained = dataObtained
    assert(emptyListOfList equals obtained)
  }

  test("Encode Rep Of Two List Of List") {
    val twoListOfList = RepOfTwoListOfList()
    val expected = "c7c0c1c0c3c0c1c0"
    val data = encode(twoListOfList)(RepOfTwoListOfList.encDec)
    assert(expected == Hex.toHexString(data))
    val dataObtained = decode[RepOfTwoListOfList](data)
    val obtained = dataObtained
    assert(twoListOfList equals obtained)
  }

  test("https://github.com/ethereum/tests/blob/master/rlptest.txt") {
    for (input: (RLPEncodeable, String) <- rlpTestData) {
      val data = RLP.encode(input._1)
      assert(input._2 == Hex.toHexString(data))
      val dataObtained = RLP.rawDecode(data)
      val obtained: RLPEncodeable = dataObtained
      val encodedAgain = RLP.encode(obtained)
      assert(data sameElements encodedAgain)
    }
  }

  test("SimpleBlock encoding") {
    val tx0 = TestSimpleTransaction(1, "cat")
    val tx1 = TestSimpleTransaction(2, "dog")

    val block = TestSimpleBlock(127, -127: Short, "horse", 1000, Seq(tx0, tx1), Seq(1, 2))
    val data = encode(block)(TestSimpleBlock.encDec)
    val dataObtained = decode[TestSimpleBlock](data)
    val obtained: TestSimpleBlock = dataObtained
    assert(block equals obtained)
  }

  test("Partial Data Parse Test") {
    val hex: String = "000080c180000000000000000000000042699b1104e93abf0008be55f912c2ff"
    val data = Hex.decode(hex)
    val decoded: Seq[Int] = decode[Seq[Int]](data.splitAt(3)._2)
    assert(1 == decoded.length)
    assert(0 == decoded.head)
  }

  test("Multiple partial decode") {
    val seq1 = RLPList("cat", "dog")
    val seq2 = RLPList(23, 10, 1986)
    val seq3 = RLPList("cat", "Lorem ipsum dolor sit amet, consectetur adipisicing elit")
    val data = Seq(RLP.encode(seq1), RLP.encode(seq2), RLP.encode(seq3)).reduce(_ ++ _)

    val decoded1 = decode[Seq[String]](data)
    assert(decoded1 equals "cat" :: "dog" :: Nil)

    val secondItemIndex = nextElementIndex(data, 0)
    val decoded2 = decode[Seq[Int]](data.drop(secondItemIndex))
    assert(decoded2 equals 23 :: 10 :: 1986 :: Nil)

    val thirdItemIndex = nextElementIndex(data, secondItemIndex)
    val decoded3 = decode[Seq[String]](data.drop(thirdItemIndex))
    assert(decoded3 equals Seq("cat", "Lorem ipsum dolor sit amet, consectetur adipisicing elit"))
  }

  implicit def emptySeqEncDec: RLPEncoder[Seq[Any]] with RLPDecoder[Seq[Any]] = new RLPEncoder[Seq[Any]]
    with RLPDecoder[Seq[Any]] {
    override def encode(obj: Seq[Any]): RLPEncodeable = RLPList()

    override def decode(rlp: RLPEncodeable): Seq[Any] = rlp match {
      case l: RLPList if l.items.isEmpty => Seq()
      case _ => throw new Exception("src is not an empty Seq")
    }
  }

  implicit val stringSeqEncDec = new RLPEncoder[Seq[String]] with RLPDecoder[Seq[String]] {
    override def encode(strings: Seq[String]): RLPEncodeable = RLPList(strings.map(stringEncDec.encode): _*)

    override def decode(rlp: RLPEncodeable): Seq[String] = rlp match {
      case l: RLPList => l.items.map(item => item: String)
      case _ => throw new RuntimeException("Invalid String Seq Decoder")
    }
  }

  implicit def stringSeqFromEncodeable(rlp: RLPEncodeable)(implicit dec: RLPDecoder[Seq[String]]): Seq[String] =
    dec.decode(rlp)

  implicit val intSeqEncDec = new RLPEncoder[Seq[Int]] with RLPDecoder[Seq[Int]] {
    override def encode(ints: Seq[Int]): RLPEncodeable = ints: RLPList

    override def decode(rlp: RLPEncodeable): Seq[Int] = rlp match {
      case l: RLPList => l.items.map(item => item: Int)
      case _ => throw new RuntimeException("Invalid Int Seq Decoder")
    }
  }

  implicit def intSeqFromEncodeable(rlp: RLPEncodeable)(implicit dec: RLPDecoder[Seq[Int]]): Seq[Int] = dec.decode(rlp)

  case class MultiList1(number: Int, seq1: Seq[String], string: String, seq2: Seq[Int])

  object MultiList1 {
    implicit val encDec = new RLPEncoder[MultiList1] with RLPDecoder[MultiList1] {
      override def encode(obj: MultiList1): RLPEncodeable = {
        import obj._
        RLPList(number, seq1, string, seq2)
      }

      override def decode(rlp: RLPEncodeable): MultiList1 = rlp match {
        case l: RLPList => MultiList1(l.items.head, l.items(1), l.items(2), l.items(3))
        case _ => throw new RuntimeException("Invalid Int Seq Decoder")
      }
    }
  }

  case class MultiList2(seq1: Seq[String], seq2: Seq[Int], seq3: Seq[Any] = Seq())

  object MultiList2 {
    implicit val encDec = new RLPEncoder[MultiList2] with RLPDecoder[MultiList2] {
      override def encode(obj: MultiList2): RLPEncodeable = {
        import obj._
        RLPList(seq1, seq2, seq3)
      }

      override def decode(rlp: RLPEncodeable): MultiList2 = rlp match {
        case l: RLPList => MultiList2(l.items.head, l.items(1), emptySeqEncDec.decode(l.items(2)))
        case _ => throw new RuntimeException("Invalid Int Seq Decoder")
      }
    }
  }

  case class EmptyListOfList()

  object EmptyListOfList {
    val instance = Seq(RLPList(RLPList(), RLPList()), RLPList())

    implicit val encDec = new RLPEncoder[EmptyListOfList] with RLPDecoder[EmptyListOfList] {
      override def encode(obj: EmptyListOfList): RLPEncodeable = RLPList(instance: _*)

      override def decode(rlp: RLPEncodeable): EmptyListOfList = rlp match {
        case l: RLPList =>
          l.items match {
            case items if items == instance => EmptyListOfList()
            case _ => throw new RuntimeException("Invalid EmptyListOfList Decoder")
          }
        case _ => throw new RuntimeException("Invalid EmptyListOfList Decoder")
      }
    }
  }

  case class RepOfTwoListOfList()

  object RepOfTwoListOfList {
    val instance = Seq(RLPList(), RLPList(RLPList()), RLPList(RLPList(), RLPList(RLPList())))

    implicit val encDec = new RLPEncoder[RepOfTwoListOfList] with RLPDecoder[RepOfTwoListOfList] {
      override def encode(obj: RepOfTwoListOfList): RLPEncodeable = RLPList(instance: _*)

      override def decode(rlp: RLPEncodeable): RepOfTwoListOfList = rlp match {
        case l: RLPList =>
          l.items match {
            case items if items == instance => RepOfTwoListOfList()
            case _ => throw new RuntimeException("Invalid RepOfTwoListOfList Decoder")
          }
        case _ => throw new RuntimeException("Invalid RepOfTwoListOfList Decoder")
      }
    }
  }

  val rlpTestData: Seq[(RLPEncodeable, String)] = Seq(
    intEncDec.encode(0) -> "80",
    stringEncDec.encode("") -> "80",
    stringEncDec.encode("d") -> "64",
    stringEncDec.encode("cat") -> "83636174",
    stringEncDec.encode("dog") -> "83646f67",
    stringSeqEncDec.encode(Seq("cat", "dog")) -> "c88363617483646f67",
    stringSeqEncDec.encode(Seq("dog", "god", "cat")) -> "cc83646f6783676f6483636174",
    intEncDec.encode(1) -> "01",
    intEncDec.encode(10) -> "0a",
    intEncDec.encode(100) -> "64",
    intEncDec.encode(1000) -> "8203e8",
    bigIntEncDec.encode(BigInt("115792089237316195423570985008687907853269984665640564039457584007913129639935"))
      -> "a0ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
    bigIntEncDec.encode(BigInt("115792089237316195423570985008687907853269984665640564039457584007913129639936"))
      -> "a1010000000000000000000000000000000000000000000000000000000000000000"
  )

  //The following classes are used for a simplifying testing for nested objects (allowing using simple RLPEncoder and RLPDecoder)
  private case class TestSimpleTransaction(id: Int, name: String)

  private object TestSimpleTransaction {
    implicit val encDec = new RLPEncoder[TestSimpleTransaction] with RLPDecoder[TestSimpleTransaction] {
      override def encode(obj: TestSimpleTransaction): RLPEncodeable = {
        import obj._
        RLPList(id, name)
      }

      override def decode(rlp: RLPEncodeable): TestSimpleTransaction = rlp match {
        case RLPList(id, name) => TestSimpleTransaction(id, name)
        case _ => throw new RuntimeException("Invalid Simple Transaction")
      }
    }

    implicit def fromEncodeable(rlp: RLPEncodeable)(implicit
        dec: RLPDecoder[TestSimpleTransaction]
    ): TestSimpleTransaction = dec.decode(rlp)
  }

  private case class TestSimpleBlock(
      id: Byte,
      parentId: Short,
      owner: String,
      nonce: Int,
      txs: Seq[TestSimpleTransaction],
      unclesIds: Seq[Int]
  )

  private object TestSimpleBlock {
    implicit val encDec = new RLPEncoder[TestSimpleBlock] with RLPDecoder[TestSimpleBlock] {
      override def encode(obj: TestSimpleBlock): RLPEncodeable = {
        import obj._
        RLPList(
          id,
          parentId,
          owner,
          nonce,
          RLPList(txs.map(TestSimpleTransaction.encDec.encode): _*),
          RLPList(unclesIds.map(id => id: RLPEncodeable): _*)
        )
      }

      override def decode(rlp: RLPEncodeable): TestSimpleBlock = rlp match {
        case RLPList(id, parentId, owner, nonce, (txs: RLPList), (unclesIds: RLPList)) =>
          TestSimpleBlock(
            id,
            parentId,
            owner,
            nonce,
            txs.items.map(TestSimpleTransaction.encDec.decode),
            unclesIds.items.map(intEncDec.decode)
          )
        case _ => throw new Exception("Can't transform RLPEncodeable to block")
      }
    }
  }
}
