# Generated for Nova V2 private accounts and Close Friends.

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0018_sharing_reposts"),
    ]

    operations = [
        migrations.CreateModel(
            name="AccountPrivacy",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("is_private", models.BooleanField(default=False)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("user", models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name="account_privacy", to="accounts.user")),
            ],
        ),
        migrations.CreateModel(
            name="FollowRequest",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("requester", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="follow_requests_sent", to="accounts.user")),
                ("target", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="follow_requests_received", to="accounts.user")),
            ],
            options={"ordering": ("-created_at", "-id")},
        ),
        migrations.CreateModel(
            name="CloseFriend",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("member", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="close_friend_memberships", to="accounts.user")),
                ("owner", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="close_friends_created", to="accounts.user")),
            ],
            options={"ordering": ("-created_at", "-id")},
        ),
        migrations.AddField(
            model_name="story",
            name="audience",
            field=models.CharField(choices=[("followers", "Followers"), ("close_friends", "Close friends")], default="followers", max_length=16),
        ),
        migrations.AddConstraint(
            model_name="followrequest",
            constraint=models.UniqueConstraint(fields=("requester", "target"), name="unique_follow_request"),
        ),
        migrations.AddConstraint(
            model_name="followrequest",
            constraint=models.CheckConstraint(condition=~models.Q(("requester", models.F("target"))), name="prevent_self_follow_request"),
        ),
        migrations.AddIndex(
            model_name="followrequest",
            index=models.Index(fields=["target", "-created_at"], name="follow_req_target_time_idx"),
        ),
        migrations.AddIndex(
            model_name="followrequest",
            index=models.Index(fields=["requester", "target"], name="follow_req_pair_idx"),
        ),
        migrations.AddConstraint(
            model_name="closefriend",
            constraint=models.UniqueConstraint(fields=("owner", "member"), name="unique_close_friend"),
        ),
        migrations.AddConstraint(
            model_name="closefriend",
            constraint=models.CheckConstraint(condition=~models.Q(("owner", models.F("member"))), name="prevent_self_close_friend"),
        ),
        migrations.AddIndex(
            model_name="closefriend",
            index=models.Index(fields=["owner", "-created_at"], name="close_friend_owner_time_idx"),
        ),
        migrations.AddIndex(
            model_name="closefriend",
            index=models.Index(fields=["owner", "member"], name="close_friend_pair_idx"),
        ),
        migrations.AddIndex(
            model_name="story",
            index=models.Index(fields=["author", "audience", "-created_at"], name="story_author_audience_idx"),
        ),
    ]
