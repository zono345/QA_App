package jp.techacademy.yusuke.shimozono.qa_app

import java.io.Serializable
import java.util.ArrayList

class Question (
    val title: String, // Firebaseから取得したタイトル
    val body: String, // Firebaseから取得した質問本文
    val name: String, // Firebaseから取得した質問者の名前
    val uid: String, // Firebaseから取得した質問者のUID
    val questionUid: String, // Firebaseから取得した質問のUID
    val genre: Int, // 質問のジャンル
    bytes: ByteArray,
    val answers: ArrayList<Answer>, // Firebaseから取得した質問のモデルクラスであるAnswerのArrayList


    // TODO 課題用の追記  いらなさそうなので、一旦コメントアウト
//    val favorites: ArrayList<Favorite>, // お気に入り登録状態


    ): Serializable {
        val imageBytes: ByteArray // Firebaseから取得した画像をbyte型の配列にしたもの

        init {
            imageBytes = bytes.clone()
        }
    }

