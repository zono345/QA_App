package jp.techacademy.yusuke.shimozono.qa_app

import java.io.Serializable

// Questionクラスの中にFavoriteクラスをArrayListで作成するので、questionUidは不要 TODO コメント要修正
// ユーザーIDとお気に入り状態がセットで保存されていればOK
class Favorite (
    val genre: String,
    val uid: String,
    val questionUid: String,
//    val favorite_state: String, // TODO 課題対応中 これ変える必要あるかも
): Serializable