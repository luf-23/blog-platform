import { useUserInfoStore } from "../../store/userInfo";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
//import { ElMessage } from "element-plus";
export function check() {
  const userInfoStore = useUserInfoStore();
  const { userInfo } = storeToRefs(userInfoStore);
  const username = userInfo.value.username;
  const router = useRouter();
  if (username !== "admin") {
    //ElMessage.warning("权限不足");
    router.push({
      path: "You_don't_have_the_Admin's_rights."
    });
    return false;
  }
  return true;
}
