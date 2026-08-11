import shutil
import tempfile

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .comment_reply_models import PostCommentReply, ReelCommentReply
from .models import Comment, Notification, Post, User
from .reels_models import Reel, ReelComment


class CommentReplyTests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-comment-replies-")
        cls._media_override = override_settings(MEDIA_ROOT=cls._media_dir)
        cls._media_override.enable()
        super().setUpClass()

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        cls._media_override.disable()
        shutil.rmtree(cls._media_dir, ignore_errors=True)

    def setUp(self):
        self.owner = self.make_user("owner")
        self.commenter = self.make_user("commenter")
        self.replier = self.make_user("replier")
        self.post = Post.objects.create(
            author=self.owner,
            image=SimpleUploadedFile("post.jpg", b"nova-post-image", content_type="image/jpeg"),
            caption="Post thread",
        )
        self.reel = Reel.objects.create(
            author=self.owner,
            video=SimpleUploadedFile("reel.mp4", b"nova-reel-video", content_type="video/mp4"),
            caption="Reel thread",
        )

    def make_user(self, suffix):
        return User.objects.create_user(
            email=f"{suffix}-reply@example.com",
            username=f"{suffix}_reply",
            password="StrongNovaPass2026!",
            name=f"{suffix.title()} Reply",
        )

    def auth(self, user):
        self.client.force_authenticate(user=user)

    def test_post_reply_is_nested_and_notifies_comment_owner(self):
        parent = Comment.objects.create(
            post=self.post,
            author=self.commenter,
            body="Parent post comment",
        )
        self.auth(self.replier)

        response = self.client.post(
            reverse("post-comments", args=[self.post.pk]),
            {"body": "Post reply", "parent_id": parent.pk},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["comment"]["parent_id"], parent.pk)
        reply = PostCommentReply.objects.get(comment=parent, author=self.replier)
        notification = Notification.objects.get(kind="comment_reply", actor=self.replier)
        self.assertEqual(notification.recipient_id, self.commenter.pk)
        self.assertEqual(notification.post_id, self.post.pk)
        self.assertEqual(notification.comment_id, parent.pk)
        self.assertEqual(notification.dedupe_key, f"comment_reply:{reply.pk}")

        thread = self.client.get(reverse("post-comments", args=[self.post.pk]))
        self.assertEqual(thread.status_code, status.HTTP_200_OK)
        self.assertEqual(thread.data["results"][0]["replies_count"], 1)
        self.assertEqual(thread.data["results"][0]["replies"][0]["body"], "Post reply")
        self.assertEqual(thread.data["results"][0]["replies"][0]["parent_id"], parent.pk)

    def test_replying_to_own_post_comment_does_not_notify_self(self):
        parent = Comment.objects.create(
            post=self.post,
            author=self.commenter,
            body="My comment",
        )
        self.auth(self.commenter)

        response = self.client.post(
            reverse("post-comments", args=[self.post.pk]),
            {"body": "Reply to myself", "parent_id": parent.pk},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertFalse(Notification.objects.filter(kind="comment_reply").exists())

    def test_post_reply_can_be_deleted_without_deleting_parent(self):
        parent = Comment.objects.create(
            post=self.post,
            author=self.commenter,
            body="Keep me",
        )
        reply = PostCommentReply.objects.create(
            comment=parent,
            author=self.replier,
            body="Remove me",
        )
        Notification.objects.create(
            recipient=self.commenter,
            actor=self.replier,
            kind="comment_reply",
            post=self.post,
            comment=parent,
            dedupe_key=f"comment_reply:{reply.pk}",
        )
        self.auth(self.replier)

        response = self.client.delete(reverse("comment-reply-detail", args=[reply.pk]))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(Comment.objects.filter(pk=parent.pk).exists())
        self.assertFalse(PostCommentReply.objects.filter(pk=reply.pk).exists())
        self.assertFalse(Notification.objects.filter(dedupe_key=f"comment_reply:{reply.pk}").exists())

    def test_deleting_parent_post_comment_cascades_replies_and_activity(self):
        parent = Comment.objects.create(
            post=self.post,
            author=self.commenter,
            body="Delete thread",
        )
        reply = PostCommentReply.objects.create(
            comment=parent,
            author=self.replier,
            body="Child reply",
        )
        Notification.objects.create(
            recipient=self.commenter,
            actor=self.replier,
            kind="comment_reply",
            post=self.post,
            comment=parent,
            dedupe_key=f"comment_reply:{reply.pk}",
        )
        self.auth(self.commenter)

        response = self.client.delete(reverse("comment-detail", args=[parent.pk]))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(Comment.objects.filter(pk=parent.pk).exists())
        self.assertFalse(PostCommentReply.objects.filter(pk=reply.pk).exists())
        self.assertFalse(Notification.objects.filter(dedupe_key=f"comment_reply:{reply.pk}").exists())

    def test_reel_reply_is_nested_and_notifies_comment_owner(self):
        parent = ReelComment.objects.create(
            reel=self.reel,
            author=self.commenter,
            body="Parent Reel comment",
        )
        self.auth(self.replier)

        response = self.client.post(
            reverse("reel-comments", args=[self.reel.pk]),
            {"body": "Reel reply", "parent_id": parent.pk},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["comment"]["parent_id"], parent.pk)
        reply = ReelCommentReply.objects.get(comment=parent, author=self.replier)
        notification = Notification.objects.get(kind="reel_reply", actor=self.replier)
        self.assertEqual(notification.recipient_id, self.commenter.pk)
        self.assertEqual(notification.dedupe_key, f"reel_reply:{reply.pk}:{self.reel.pk}")

        thread = self.client.get(reverse("reel-comments", args=[self.reel.pk]))
        self.assertEqual(thread.status_code, status.HTTP_200_OK)
        self.assertEqual(thread.data["results"][0]["replies_count"], 1)
        self.assertEqual(thread.data["results"][0]["replies"][0]["body"], "Reel reply")

    def test_reel_reply_activity_points_to_real_reel_owner(self):
        parent = ReelComment.objects.create(
            reel=self.reel,
            author=self.commenter,
            body="Reply here",
        )
        self.auth(self.replier)
        created = self.client.post(
            reverse("reel-comments", args=[self.reel.pk]),
            {"body": "Third party reply", "parent_id": parent.pk},
            format="json",
        )
        self.assertEqual(created.status_code, status.HTTP_201_CREATED)

        self.auth(self.commenter)
        activity = self.client.get(reverse("notifications"))

        self.assertEqual(activity.status_code, status.HTTP_200_OK)
        row = next(item for item in activity.data["results"] if item["kind"] == "reel_reply")
        self.assertEqual(row["reel_id"], self.reel.pk)
        self.assertEqual(row["reel_author_username"], self.owner.username)

    def test_replying_to_own_reel_comment_does_not_notify_self(self):
        parent = ReelComment.objects.create(
            reel=self.reel,
            author=self.commenter,
            body="My Reel comment",
        )
        self.auth(self.commenter)

        response = self.client.post(
            reverse("reel-comments", args=[self.reel.pk]),
            {"body": "Reply to myself", "parent_id": parent.pk},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertFalse(Notification.objects.filter(kind="reel_reply").exists())

    def test_deleting_reel_reply_cleans_reply_activity(self):
        parent = ReelComment.objects.create(
            reel=self.reel,
            author=self.commenter,
            body="Parent",
        )
        reply = ReelCommentReply.objects.create(
            comment=parent,
            author=self.replier,
            body="Delete me",
        )
        Notification.objects.create(
            recipient=self.commenter,
            actor=self.replier,
            kind="reel_reply",
            dedupe_key=f"reel_reply:{reply.pk}:{self.reel.pk}",
        )
        self.auth(self.replier)

        response = self.client.delete(reverse("reel-comment-reply-detail", args=[reply.pk]))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(ReelCommentReply.objects.filter(pk=reply.pk).exists())
        self.assertFalse(
            Notification.objects.filter(dedupe_key=f"reel_reply:{reply.pk}:{self.reel.pk}").exists()
        )
