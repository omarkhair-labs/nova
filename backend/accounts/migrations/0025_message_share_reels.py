from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0024_comment_replies"),
    ]

    operations = [
        migrations.AddField(
            model_name="messageshare",
            name="reel",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.SET_NULL,
                related_name="message_shares",
                to="accounts.reel",
            ),
        ),
        migrations.AlterField(
            model_name="messageshare",
            name="kind",
            field=models.CharField(
                choices=[("post", "Post"), ("profile", "Profile"), ("reel", "Reel")],
                max_length=16,
            ),
        ),
        migrations.RemoveConstraint(
            model_name="messageshare",
            name="message_share_matches_kind",
        ),
        migrations.AddConstraint(
            model_name="messageshare",
            constraint=models.CheckConstraint(
                condition=(
                    models.Q(("kind", "post"), ("profile__isnull", True), ("reel__isnull", True))
                    | models.Q(("kind", "profile"), ("post__isnull", True), ("reel__isnull", True))
                    | models.Q(("kind", "reel"), ("post__isnull", True), ("profile__isnull", True))
                ),
                name="message_share_matches_kind",
            ),
        ),
    ]
